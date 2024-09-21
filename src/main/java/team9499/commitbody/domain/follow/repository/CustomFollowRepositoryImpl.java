package team9499.commitbody.domain.follow.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.Member.domain.AccountStatus;
import team9499.commitbody.domain.follow.domain.Follow;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.domain.follow.domain.QFollow;
import team9499.commitbody.domain.follow.dto.FollowDto;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.Member.domain.QMember.*;
import static team9499.commitbody.domain.follow.domain.FollowType.FOLLOW_ONLY;
import static team9499.commitbody.domain.follow.domain.QFollow.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomFollowRepositoryImpl implements CustomFollowRepository{


    private final JPAQueryFactory jpaQueryFactory;

    private final String FULL_TEXT_INDEX ="function('match',{0},{1})";
    private final String FOLLOWING = "following";

    /**
     * 해당 사용자가 팔로잉한 사용자의 목록을 조회합니다.
     * @param followerId 조회할 사용자
     */
    @Override
    public Slice<FollowDto> getAllFollowings(Long followerId, Long followingId, String nickName, Long lastId, Pageable pageable) {
        BooleanBuilder builder = lastIdBuilder(lastId);
        nicknameBuilder(nickName, builder);

        List<Follow> followList = jpaQueryFactory.select(follow)
                .from(follow)
                .join(follow.following,member).fetchJoin()
                .where(builder,follow.follower.id.eq(followingId).and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW))))
                .limit(pageable.getPageSize()+1)
                .orderBy(follow.id.asc())
                .fetch();

        List<FollowDto> followingDtoList = new ArrayList<>();
        if (followerId != followingId) {// 상대방 계정 조회시
            validPrivateAccount(followerId,followingId,followList.get(0).getFollower().getAccountStatus()); // 해당 사용자가 팔로우 상태인지 확인
            followingDtoList = getFriendFollows(followerId, followList,FOLLOWING);
        }else {
            followingDtoList = getMyFollows(followList,FOLLOWING);
        }

        boolean hasNext = false;
        if (followingDtoList.size() > pageable.getPageSize()){
            hasNext = true;
            followingDtoList.remove(pageable.getPageSize());
        }
        return new SliceImpl<>(followingDtoList,pageable,hasNext);
    }

    /**
     * 해당 사용자를 팔로워 하는 목록을 조회
     * @param followerId 현재 로그인한 사용자
     * @param followId 조회할 사용자 Id
     */
    @Override
    public Slice<FollowDto> getAllFollowers(Long followerId, Long followId, String nickName, Long lastId, Pageable pageable) {
        BooleanBuilder builder = lastIdBuilder(lastId);
        nicknameBuilder(nickName,builder);

        List<Follow> followList= jpaQueryFactory.select(follow)
                .from(follow)
                .join(follow.follower, member).fetchJoin()
                .where(builder,follow.following.id.eq(followId).and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW))))  // 올바른 조건
                .limit(pageable.getPageSize()+1)
                .orderBy(follow.id.asc())
                .fetch();
        List<FollowDto> followerDtoList = new ArrayList<>();
        if (followerId != followId) {       // 상대방 계정 조회시
            validPrivateAccount(followerId, followId,followList.get(0).getFollowing().getAccountStatus());  // 해당 사용자가 팔로우 상태인지 확인
            followerDtoList = getFriendFollows(followerId, followList,"follower");
        }else {
            followerDtoList = getMyFollows(followList,"follower");
        }

        boolean hasNext = false;
        if (followerDtoList.size() > pageable.getPageSize()){
            hasNext= true;
            followerDtoList.remove(pageable.getPageSize());
        }
        return new SliceImpl<>(followerDtoList,pageable,hasNext);
    }

    @Override
    public long getCountFollowing(Long followerId) {
        return jpaQueryFactory.select(follow.count())
                .from(follow)
                .join(member).on(member.id.eq(follow.following.id))
                .where(follow.follower.id.eq(followerId).and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW)))).fetchOne();
    }

    @Override
    public long getCountFollower(Long followingId) {

        return jpaQueryFactory.select(follow.count())
                .from(follow)
                .join(member).on(member.id.eq(follow.follower.id))  // 올바른 조인 조건
                .where(follow.following.id.eq(followingId).and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW))))  // 올바른 조건
                .fetchOne();
    }

    /**
     * 상대 페이지 검색시에 사용되는 팔로우 상태를 검사합니다.
     * FOLLOW_ONLY - 상대방만 팔로우한상태
     * NEITHER - 둘다 팔로우를 하지 않았을때
     * FOLLOW - 둘다 팔로우 상태
     * @return FollowType을 반환합니다.
     */
    @Override
    public FollowType followStatus(Long followerId, Long followingId) {
        FollowType type = null;
        FollowStatus followStatus = jpaQueryFactory.select(follow.status)
                .from(follow)
                .where(follow.follower.id.eq(followerId).and(follow.following.id.eq(followingId)))
                .fetchOne();
        //팔로우가 되어 있지 않았을때 검증
        if (followStatus==null || followStatus.equals(FollowStatus.CANCEL) || followStatus.equals(FollowStatus.UNFOLLOW)){
            FollowStatus followStatus1 = jpaQueryFactory.select(follow.status)
                    .from(follow)
                    .where(follow.follower.id.eq(followingId).and(follow.following.id.eq(followerId))).fetchOne();
            if (followStatus1 == null || followStatus1.equals(FollowStatus.CANCEL)){    // 상대방도 팔로우를 하지않았을경우는 NEITHER
                type = FollowType.NEITHER;
            }else if (followStatus1.equals(FollowStatus.FOLLOWING)){        // 상대방만 사용자를 팔로우중이라면 맞팔로우
                type = FOLLOW_ONLY;
            }
        }
        else if (followStatus.equals(FollowStatus.FOLLOWING)|| followStatus.equals(FollowStatus.MUTUAL_FOLLOW))       // 팔로우 상태
            type = FollowType.FOLLOW;
        else if (followStatus.equals(FollowStatus.REQUEST)) {   // 상대방이 팔로우 요청을 보낸 상태라면
            type = FOLLOW_ONLY;
        }

        return type;
    }

    @Override
    public void cancelFollow(Long followerId, Long followingId) {
        jpaQueryFactory.update(follow)
                .set(follow.status,FollowStatus.CANCEL)
                .where(follow.follower.id.eq(followerId).and(follow.following.id.eq(followingId)))
                .execute();

        jpaQueryFactory.update(follow)
                .set(follow.status,FollowStatus.CANCEL)
                .where(follow.follower.id.eq(followingId).and(follow.following.id.eq(followerId)))
                .execute();
    }

    /**
     * 팔로잉한 사용자의 ID를 반환
     * @param followerId 팔로워 ID
     * @return 팔로잉 사용자 목록
     */
    @Override
    public List<Long> followings(Long followerId) {
        return jpaQueryFactory.select(follow.following.id)
                .from(follow)
                .where(follow.follower.id.eq(followerId).and(follow.status.eq(FollowStatus.MUTUAL_FOLLOW).or(follow.status.eq(FollowStatus.FOLLOWING)))).fetch();
    }

    private static BooleanBuilder lastIdBuilder(Long lastId) {
        BooleanBuilder builder = new BooleanBuilder();
        if (lastId !=null){
            builder.and(follow.id.gt(lastId));
        }
        return builder;
    }

    private void nicknameBuilder(String nickName, BooleanBuilder builder) {
        if (nickName != null) {
            NumberTemplate<Double> doubleNumberTemplate = Expressions.numberTemplate(Double.class,
                    FULL_TEXT_INDEX, member.nickname,"+"+ nickName +"+");
            builder.and(doubleNumberTemplate.gt(0));
        }
    }

    /**
     * 팔로워 목록에서 현재 사용자가 조회된사용자를 팔로워 하는지 체크
     * @param follow  현재 조호된 객체
     * @param followerId    현재 로그인한 사용자
     */
    private boolean checkFollow(Follow follow, Long followerId,String type) {
        BooleanBuilder builder = new BooleanBuilder();
        if (type.equals(FOLLOWING)){
            builder.and(QFollow.follow.follower.id.eq(followerId).and(QFollow.follow.following.id.eq(follow.getFollowing().getId())));
        }else{
            builder.and(QFollow.follow.following.id.eq(follow.getFollower().getId()).and(QFollow.follow.follower.id.eq(followerId)));
        }
        FollowStatus followStatus = jpaQueryFactory.select(QFollow.follow.status)
                .from(QFollow.follow)
                .where(builder).fetchOne();
        if (followStatus!=null && (followStatus.equals(FollowStatus.FOLLOWING)|| followStatus.equals(FollowStatus.MUTUAL_FOLLOW))){
            return true;
        }
        return false;
    }

    private static boolean checkFollow(FollowStatus followStatus){
        return followStatus.equals(FollowStatus.FOLLOWING) || followStatus.equals(FollowStatus.MUTUAL_FOLLOW)? true : false;
    }

    /**
     * 현재 사용자의 팔로워/팔로잉 목록을조회합니다.
     * @param followList 조회된 팔로워/팔로잉 목록
     * @param type  following/ follower
     */
    private List<FollowDto> getMyFollows(List<Follow> followList, String type) {
        return followList.stream()
                .map(follow -> new FollowDto(
                        follow.getId(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getId() :follow.getFollower().getId(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getNickname() :follow.getFollower().getNickname(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getProfile() :follow.getFollower().getProfile(),
                        checkFollow(follow.getStatus()))
                )
                .collect(Collectors.toList());
    }

    /**
     * 상대방의 팔로워/팔로잉 목록을 조회합니다.
     * @param followerId    현재 사용자 id
     * @param followList    조회된 팔러워/팔로잉 목록
     * @param type  following/ follower
     */
    private List<FollowDto> getFriendFollows(Long followerId, List<Follow> followList, String type) {
        return followList.stream()
                .sorted((f1, f2) -> {
                    // 현재 사용자가 아닌 경우 기본 정렬 유지
                    if (type.equals(FOLLOWING)){
                        // 현재 사용자의 ID와 비교하여 정렬: 현재 사용자는 맨 위로
                        if (f1.getFollowing().getId().equals(followerId)) return -1;  // 현재 사용자는 -1로 반환하여 맨 위로
                        if (f2.getFollowing().getId().equals(followerId)) return 1;   // 비교하는 대상은 아래로
                    }else {
                        // 현재 사용자의 ID와 비교하여 정렬: 현재 사용자는 맨 위로
                        if (f1.getFollower().getId().equals(followerId)) return -1;  // 현재 사용자는 -1로 반환하여 맨 위로
                        if (f2.getFollower().getId().equals(followerId)) return 1;   // 비교하는 대상은 아래로
                    }
                    return 0;
                })
                .map(follow -> new FollowDto(
                        follow.getId(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getId() :follow.getFollower().getId(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getNickname() :follow.getFollower().getNickname(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getProfile() :follow.getFollower().getProfile(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getId() == followerId ? null: checkFollow(follow, followerId,type) : follow.getFollower().getId() == followerId ? null:checkFollow(follow, followerId,type) ,
                        type.equals(FOLLOWING) ? follow.getFollowing().getId().equals(followerId) ? true : null : follow.getFollower().getId().equals(followerId) ? true : null )
                )
                .collect(Collectors.toList());
    }

    /*
    현재 조회할 사용자가 비공개 계정일때 맞팔로우 상태가 아니라면 예외 발생
     */
    private void validPrivateAccount(Long followerId, Long followId,AccountStatus accountStatus) {
        FollowStatus followStatus = jpaQueryFactory.select(follow.status).from(follow).where(follow.following.id.eq(followerId).and(follow.follower.id.eq(followId))).fetchOne();
        if (!followStatus.equals(FollowStatus.MUTUAL_FOLLOW) && accountStatus.equals(AccountStatus.PRIVATE)) throw new InvalidUsageException(ExceptionStatus.BAD_REQUEST,ExceptionType.PRIVATE_ACCOUNT);
    }
}
