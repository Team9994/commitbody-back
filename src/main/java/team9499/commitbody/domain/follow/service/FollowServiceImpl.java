package team9499.commitbody.domain.follow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.follow.domain.Follow;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.domain.follow.dto.FollowDto;
import team9499.commitbody.domain.follow.dto.response.FollowResponse;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.constants.FollowConstants;
import team9499.commitbody.global.redis.RedisService;

import java.util.Optional;

import static team9499.commitbody.domain.follow.domain.FollowStatus.*;
import static team9499.commitbody.global.constants.Delimiter.*;
import static team9499.commitbody.global.constants.FollowConstants.*;


@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final RedisService redisService;

    /**
     * 팔로워 메서드
     * 1. 팔로워 요청
     * 2. 팔로잉 요청
     * 3. 맞팔로잉
     * 4. 언팔로잉
     * 5. 팔로워 취소
     * 하나의 메서드를 통해 5가지 기능을 동적으로 작동하도록 구현
     *
     * @param followerId  팔로워 기능을 요청 사용자
     * @param followingId 팔로워 기능을 받는 사용자
     * @param type        FOLLOW,UNFOLLOW 을 통해 언팔, 팔로워 구분
     * @return (맞팔로우, 언팔로우, 팔로우 취소, 팔로우 요청) 요청 타입에 맞게 반환
     */
    @Override

    public String follow(Long followerId, Long followingId, FollowType type) {
        String status = "";
        Member follower = getRedisMember(followerId);
        Member following = getRedisMember(followingId);
        Follow toFollow = Follow.create(follower, following, FollowType.FOLLOWING);
        Follow fromFollow = Follow.create(following, follower, FollowType.FOLLOW);
        status = handleFollowRelationship(followerId, followingId, toFollow, type, TO);   // 'FOLLOWING' 관계 처리
        handleFollowRelationship(followingId, followerId, fromFollow, type, FROM);     // 'FOLLOW' 관계 처리
        return status;
    }

    /**
     * 팔로워 목록 조회
     *
     * @param followingId 조회할 팔로잉 ID
     * @param nickName    검색할 사용자 명(required : false)
     * @param lastId      마지막 팔로워 ID 값(required : false)
     * @param pageable    페이징 정보
     * @return 검색한 데이터를 FollowResponse 객체러로 매핑후 반환
     */
    @Transactional(readOnly = true)
    @Override
    public FollowResponse getFollowers(Long followingId, Long followerId, String nickName, Long lastId, Pageable pageable) {
        Slice<FollowDto> allFollowers = followRepository.getAllFollowers(followingId, followerId, nickName, lastId, pageable);
        return new FollowResponse(allFollowers.hasNext(), allFollowers.getContent());
    }

    /**
     * 팔로워 목록 조회
     *
     * @param followerId 조회할 팔로워 ID
     * @param nickName   검색할 사용자 닉네임(required : false)
     * @param lastId     마지막 팔로워 ID 값(required : false)
     * @param pageable   페이징 정보
     * @return 검색한 데이터를 FollowResponse 객체러로 매핑후 반환
     */
    @Transactional(readOnly = true)
    @Override
    public FollowResponse getFollowings(Long followerId, Long followingId, String nickName, Long lastId, Pageable pageable) {
        Slice<FollowDto> allFollowings = followRepository.getAllFollowings(followerId, followingId, nickName, lastId, pageable);
        return new FollowResponse(allFollowings.hasNext(), allFollowings.getContent());
    }

    /**
     * 사용자 차단시 서로 팔로우 취소상태로 변경
     */
    @Async
    @Override
    public void cancelFollow(Long followerId, Long followingId, String status) {
        if (status.equals(SUCCESS_BLOCK)) {
            followRepository.cancelFollow(followerId, followingId);
        }
    }

    /*
    사용자 정보를 조회 redis에서 먼저 사용자 정보를 조회후 없을시에 db조회 후 저장후 member를 반환
     */
    private Member getRedisMember(Long followId) {
        return redisService.getMemberDto(followId.toString()).get();
    }

    private void retryRequestException(FollowType type, String direction, Optional<Follow> existingFollow) {
        if (existingFollow.isPresent() &&
                existingFollow.get().getStatus().equals(typeToStatus(type, direction))) {
            throw new InvalidUsageException(ExceptionStatus.BAD_REQUEST, ExceptionType.ALREADY_REQUESTED);
        }
    }

    /*
    해당 요청 타입의따라 팔로잉 상태를 String 값으로 반환
     */
    private String handleFollowRelationship(Long followerId, Long followingId, Follow follow, FollowType type, String direction) {
        Optional<Follow> existingFollow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        retryRequestException(type, direction, existingFollow);

        if (type.equals(FollowType.UNFOLLOW)) {     // 언팔로우 이면
            if (existingFollow.isPresent()) {
                return processUnfollowForInitiator(direction, existingFollow.get());
            }
        }
        return processFollowRequest(follow, direction, existingFollow);
    }

    private static String processUnfollowForInitiator(String direction, Follow follow) {
        if (direction.equals(TO)) {       // 언팔하려는 사용자일때
            return handleUnFollowForInitiator(follow);
        }
        handleUnFollowStatusUpdate(follow);
        return STRING_EMPTY;
    }

    private static String handleUnFollowForInitiator(Follow follow) {
        switch (follow.getStatus()) {
            case MUTUAL_FOLLOW -> {
                follow.updateFollowStatus(FollowStatus.UNFOLLOW); // 언팔
                return FollowConstants.UNFOLLOW;
            }
            case FOLLOWING -> {
                follow.updateFollowStatus(FollowStatus.CANCEL); // 팔로우 취소
                return CANCEL_FOLLOW;
            }
            default -> {
                return STRING_EMPTY;
            }
        }
    }

    private static void handleUnFollowStatusUpdate(Follow follow) {
        switch (follow.getStatus()) {
            case MUTUAL_FOLLOW -> follow.updateFollowStatus(FOLLOWING);
            case REQUEST -> follow.updateFollowStatus(CANCEL);
            default -> follow.updateFollowStatus(CANCEL);         // 모두 취소 상태 업데이트
        }
    }

    private String processFollowRequest(Follow follow, String direction, Optional<Follow> existingFollow) {
        if (existingFollow.isEmpty()) {     // 비어 있다면 팔로우 요청
            return requestNewFollow(follow);
        }
        Follow existing = existingFollow.get(); // 팔로워 객체
        if (existing.getStatus().equals(CANCEL)) {    // 취소된 상태에서 팔로우 요청을 한다면
            return processCancelFollowRequest(direction, existing);
        }
        existing.updateFollowStatus(FollowStatus.MUTUAL_FOLLOW); // MUTUAL_FOLLOW 상태로 업데이트
        return FollowConstants.MUTUAL_FOLLOW;
    }

    private String requestNewFollow(Follow follow) {
        followRepository.save(follow);
        return REQUEST_FOLLOW;
    }

    private static String processCancelFollowRequest(String direction, Follow existing) {
        if (direction.equals(TO)) {      // 요청자일 때
            existing.updateFollowStatus(FOLLOWING); // 팔로잉 상태로 업데이트
            return REQUEST_FOLLOW;
        }
        existing.updateFollowStatus(REQUEST);   // 대상자는 요청 상태로 변경
        return STRING_EMPTY;
    }


    private FollowStatus typeToStatus(FollowType type, String direction) {
        if (type.equals(FollowType.UNFOLLOW)) {
            return direction.equals(TO) ? FollowStatus.UNFOLLOW : CANCEL;
        }
        return direction.equals(TO) ? FOLLOWING : REQUEST;
    }
}
