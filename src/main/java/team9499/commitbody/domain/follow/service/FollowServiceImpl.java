package team9499.commitbody.domain.follow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.follow.domain.Follow;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.domain.follow.dto.FollowerDto;
import team9499.commitbody.domain.follow.dto.FollowingDto;
import team9499.commitbody.domain.follow.dto.response.FollowResponse;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.redis.RedisService;

import java.time.Duration;
import java.util.Optional;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService{

    private final FollowRepository followRepository;
    private final RedisService redisService;
    private final MemberRepository memberRepository;


    private final String TO ="to";
    private final String MUTUAL_FOLLOW = "맞팔로우";
    private final String UNFOLLOW = "언팔로우";
    private final String CANCEL_FOLLOW = "팔로우 취소";
    private final String REQUEST_FOLLOW = "팔로우 요청";


    /**
     * 팔로워 메서드
     * 1. 팔로워 요청
     * 2. 팔로잉 요청
     * 3. 맞팔로잉
     * 4. 언팔로잉
     * 5. 팔로워 취소
     * 하나의 메서드를 통해 5가지 기능을 동적으로 작동하도록 구현
     * @param followerId 팔로워 기능을 요청 사용자
     * @param followingId   팔로워 기능을 받는 사용자
     * @param type  FOLLOW,UNFOLLOW 을 통해 언팔, 팔로워 구분
     * @return  (맞팔로우, 언팔로우, 팔로우 취소, 팔로우 요청) 요청 타입에 맞게 반환 
     */
    @Override
    public String follow(Long followerId, Long followingId, FollowType type) {

        String status = "";
        Member follower = getRedisMember(followerId);
        Member following = getRedisMember(followingId);

        Follow toFollow = Follow.create(follower, following, FollowType.FOLLOWING);
        Follow fromFollow = Follow.create(following, follower, FollowType.FOLLOW);

        // 'FOLLOWING' 관계 처리
        status = handleFollowRelationship(followerId, followingId, toFollow,type,TO);
        // 'FOLLOW' 관계 처리
        handleFollowRelationship(followingId, followerId, fromFollow, type, "from");

        return status;
    }

    /**
     * 팔로워 목록 조회
     * @param followingId  조회할 팔로잉 ID
     * @param nickName  검색할 사용자 명(required : false)
     * @param lastId    마지막 팔로워 ID 값(required : false)
     * @param pageable 페이징 정보
     * @return 검색한 데이터를 FollowResponse 객체러로 매핑후 반환
     */
    @Transactional(readOnly = true)
    @Override
    public FollowResponse getFollowers(Long followingId, String nickName,Long lastId, Pageable pageable) {
        Slice<FollowerDto> allFollowers = followRepository.getAllFollowers(followingId, nickName,lastId, pageable);
        return new FollowResponse(allFollowers.hasNext(),allFollowers.getContent());
    }

    /**
     * 팔로워 목록 조회
     * @param followerId 조회할 팔로워 ID
     * @param nickName  검색할 사용자 닉네임(required : false)
     * @param lastId    마지막 팔로워 ID 값(required : false)
     * @param pageable  페이징 정보
     * @return 검색한 데이터를 FollowResponse 객체러로 매핑후 반환
     */
    @Transactional(readOnly = true)
    @Override
    public FollowResponse getFollowings(Long followerId,String nickName,Long lastId, Pageable pageable) {
        Slice<FollowingDto> allFollowings = followRepository.getAllFollowings(followerId, nickName, lastId, pageable);
        return new FollowResponse(allFollowings.hasNext(),allFollowings.getContent());
    }

    /*
    사용자 정보를 조회 redis에서 먼저 사용자 정보를 조회후 없을시에 db조회 후 저장후 member를 반환
     */
    private Member getRedisMember(Long followId) {
        return redisService.getMemberDto(followId.toString()).orElseGet(() -> {
            Member member = memberRepository.findById(followId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
            redisService.setMember(member, Duration.ofDays(7));
            return member;
        });
    }

    /*
    해당 요청 타입의따라 팔로잉 상태를 String 값으로 반환
     */
    private String handleFollowRelationship(Long followerId, Long followingId, Follow follow,FollowType type,String direction) {
        String status = "";
        Optional<Follow> existingFollow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);

        if (existingFollow.isPresent() &&
                existingFollow.get().getStatus().equals(typeToStatus(type, direction))) {
            // 연속 요청 발생 시 예외 발생
            throw new InvalidUsageException(ExceptionStatus.BAD_REQUEST,ExceptionType.ALREADY_REQUESTED);
        }

        if (type.equals(FollowType.UNFOLLOW)) {     // 언팔로우 이면
            if (existingFollow.isPresent()) {
                Follow existing = existingFollow.get();
                if (direction.equals(TO)) {       // 언팔하려는 사용자일때
                    if (existing.getStatus().equals(FollowStatus.MUTUAL_FOLLOW)) { // 맞팔로우 상태일때는
                        existing.updateFollowStatus(FollowStatus.UNFOLLOW); // 언팔
                        status = UNFOLLOW;
                    }else if (existing.getStatus().equals(FollowStatus.FOLLOWING)){   // 맞팔 되어있지않고 팔로우만 한상태일때
                        existing.updateFollowStatus(FollowStatus.CANCEL);   // 팔로우 취소로 변경
                        status = CANCEL_FOLLOW;
                    }
                } else {        // 언팔 대상자
                    if (existing.getStatus().equals(FollowStatus.MUTUAL_FOLLOW))    // 맞팔상태일때
                        existing.updateFollowStatus(FollowStatus.FOLLOWING);        // 팔로워 상태로 업데이트
                    else if (existing.getStatus().equals(FollowStatus.REQUEST)){    // 요청 살태일때는
                        existing.updateFollowStatus(FollowStatus.CANCEL);   // 요청 취소
                    }else{
                        existing.updateFollowStatus(FollowStatus.CANCEL);       // 모두 취소 상태 업데이트
                    }
                }
            }
        } else {      // 팔로우 요청일 때
            if (existingFollow.isEmpty()) {     // 비어 있다면 팔로우 요청
                followRepository.save(follow);
                status = REQUEST_FOLLOW;
            } else {
                Follow existing = existingFollow.get(); // 팔로워 객체

                if (existing.getStatus().equals(FollowStatus.CANCEL)) {    // 취소된 상태에서 팔로우 요청을 한다면
                    if (direction.equals(TO)) {      // 요청자일 때
                        existing.updateFollowStatus(FollowStatus.FOLLOWING); // 팔로잉 상태로 업데이트
                        status = REQUEST_FOLLOW;  
                    } else {
                        existing.updateFollowStatus(FollowStatus.REQUEST);   // 대상자는 요청 상태로 변경
                    }
                } else {
                    existing.updateFollowStatus(FollowStatus.MUTUAL_FOLLOW); // MUTUAL_FOLLOW 상태로 업데이트
                    status = MUTUAL_FOLLOW;
                }
            }
        }
        return status;
    }

    private FollowStatus typeToStatus(FollowType type, String direction) {
        if (type.equals(FollowType.UNFOLLOW)) {
            return direction.equals(TO) ? FollowStatus.UNFOLLOW : FollowStatus.CANCEL;
        } else {
            return direction.equals(TO) ? FollowStatus.FOLLOWING : FollowStatus.REQUEST;
        }
    }
}
