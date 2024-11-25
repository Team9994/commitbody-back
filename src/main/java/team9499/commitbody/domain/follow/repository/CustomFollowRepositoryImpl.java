package team9499.commitbody.domain.follow.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.Member.domain.QMember.*;
import static team9499.commitbody.domain.follow.domain.QFollow.*;
import static team9499.commitbody.global.constants.Delimiter.PLUS;

@Repository
@RequiredArgsConstructor
public class CustomFollowRepositoryImpl implements CustomFollowRepository {

    private final String FULL_TEXT_INDEX = "function('match',{0},{1})";
    private final String FOLLOWING = "following";
    private final String FOLLOWERS = "follower";

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * 해당 사용자가 팔로잉한 사용자의 목록을 조회합니다.
     */
    @Override
    public Slice<FollowDto> getAllFollowings(Long followerId, Long followingId, String nickName,
                                             Long lastId, Pageable pageable) {
        BooleanBuilder builder = lastIdBuilder(lastId);
        nicknameBuilder(nickName, builder);
        List<Follow> followList = getFollowingsQuery(followingId, pageable, builder);
        List<FollowDto> followingDtoList = getFollowIngDtos(followerId, followingId, followList);
        return new SliceImpl<>(followingDtoList, pageable, isHasNext(pageable, followingDtoList));
    }

    /**
     * 해당 사용자를 팔로워 하는 목록을 조회
     *
     * @param followerId 현재 로그인한 사용자
     * @param followId   조회할 사용자 Id
     */
    @Override
    public Slice<FollowDto> getAllFollowers(Long followerId, Long followId, String nickName, Long lastId, Pageable pageable) {
        BooleanBuilder builder = lastIdBuilder(lastId);
        nicknameBuilder(nickName, builder);
        List<Follow> followList = getFollowersQuery(followId, pageable, builder);
        List<FollowDto> followerDtoList = getFollowerDtos(followerId, followId, followList);
        return new SliceImpl<>(followerDtoList, pageable, isHasNext(pageable, followerDtoList));
    }

    @Override
    public long getCountFollowing(Long followerId) {
        Long count = jpaQueryFactory.select(follow.count())
                .from(follow)
                .join(member).on(member.id.eq(follow.following.id))
                .where(follow.follower.id.eq(followerId)
                        .and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW)))
                        .and(follow.following.isWithdrawn.eq(false))
                ).fetchOne();
        return countNull(count);
    }

    @Override
    public long getCountFollower(Long followingId) {
        Long count = jpaQueryFactory.select(follow.count())
                .from(follow)
                .join(member).on(member.id.eq(follow.follower.id))
                .where(follow.following.id.eq(followingId)
                        .and(follow.status.eq(FollowStatus.FOLLOWING)
                                .or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW)))
                        .and(follow.follower.isWithdrawn.eq(false))
                )
                .fetchOne();
        return countNull(count);
    }

    /**
     * 상대 페이지 검색시에 사용되는 팔로우 상태를 검사합니다.
     * FOLLOW_ONLY - 상대방만 팔로우한상태
     * NEITHER - 둘다 팔로우를 하지 않았을때
     * FOLLOW - 둘다 팔로우 상태
     *
     * @return FollowType을 반환합니다.
     */
    @Override
    public FollowType followStatus(Long followerId, Long followingId) {
        FollowStatus userToTargetStatus = followStatusQuery(followerId, followingId);
        FollowStatus targetToUserStatus = followStatusQuery(followingId, followerId);
        return handleFollowStatus(userToTargetStatus, targetToUserStatus);
    }

    @Override
    public void cancelFollow(Long followerId, Long followingId) {
        jpaQueryFactory.update(follow)
                .set(follow.status, FollowStatus.CANCEL)
                .where(follow.follower.id.eq(followerId).and(follow.following.id.eq(followingId)))
                .execute();

        jpaQueryFactory.update(follow)
                .set(follow.status, FollowStatus.CANCEL)
                .where(follow.follower.id.eq(followingId).and(follow.following.id.eq(followerId)))
                .execute();
    }

    /**
     * 팔로잉한 사용자의 ID 목록을 반환
     */
    @Override
    public List<Long> followings(Long followerId) {
        return jpaQueryFactory.select(follow.following.id)
                .from(follow)
                .where(follow.follower.id.eq(followerId)
                        .and(follow.status.eq(FollowStatus.MUTUAL_FOLLOW).or(follow.status.eq(FollowStatus.FOLLOWING)))
                        .and(follow.following.isWithdrawn.eq(false))).fetch();
    }

    private static BooleanBuilder lastIdBuilder(Long lastId) {
        BooleanBuilder builder = new BooleanBuilder();
        if (lastId != null) {
            builder.and(follow.id.gt(lastId));
        }
        return builder;
    }

    private void nicknameBuilder(String nickName, BooleanBuilder builder) {
        if (nickName != null) {
            NumberTemplate<Double> doubleNumberTemplate = Expressions.numberTemplate(Double.class,
                    FULL_TEXT_INDEX, member.nickname, PLUS + nickName + PLUS);
            builder.and(doubleNumberTemplate.gt(0));
        }
    }

    private List<Follow> getFollowingsQuery(Long followingId, Pageable pageable, BooleanBuilder builder) {
        return jpaQueryFactory.select(follow)
                .from(follow)
                .join(follow.following, member).fetchJoin()
                .where(builder, follow.follower.id.eq(followingId)
                        .and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW)))
                        .and(follow.following.isWithdrawn.eq(false))
                )
                .limit(pageable.getPageSize() + 1)
                .orderBy(follow.id.asc())
                .fetch();
    }

    private List<FollowDto> getFollowIngDtos(Long followerId, Long followingId, List<Follow> followList) {
        if (!followerId.equals(followingId)) {// 상대방 계정 조회시
            if (!followList.isEmpty()) {
                validPrivateAccount(followerId, followingId, followList.get(0).getFollower().getAccountStatus()); // 해당 사용자가 팔로우 상태인지 확인
                return getFriendFollows(followerId, followList, FOLLOWING);
            }
        }
        return getMyFollows(followList, FOLLOWING);
    }

    /**
     * 상대방의 팔로워/팔로잉 목록을 조회합니다.
     */
    private List<FollowDto> getFriendFollows(Long followerId, List<Follow> followList, String type) {
        boolean isFollowingType = FOLLOWING.equals(type);
        return followList.stream()
                .sorted((f1, f2) -> compareByCurrentUser(f1, f2, followerId, isFollowingType))
                .map(follow -> mapToFollowDto(follow, followerId, isFollowingType))
                .collect(Collectors.toList());
    }

    private int compareByCurrentUser(Follow f1, Follow f2, Long followerId, boolean isFollowingType) {
        Long id1 = isFollowingType ? f1.getFollowing().getId() : f1.getFollower().getId();
        Long id2 = isFollowingType ? f2.getFollowing().getId() : f2.getFollower().getId();

        if (id1.equals(followerId)) return -1; // 현재 사용자를 맨 위로
        if (id2.equals(followerId)) return 1;  // 비교 대상은 아래로
        return 0; // 기본 정렬 유지
    }

    private FollowDto mapToFollowDto(Follow follow, Long followerId, boolean isFollowingType) {
        Long userId = isFollowingType ? follow.getFollowing().getId() : follow.getFollower().getId();
        String nickname = isFollowingType ? follow.getFollowing().getNickname() : follow.getFollower().getNickname();
        String profile = isFollowingType ? follow.getFollowing().getProfile() : follow.getFollower().getProfile();
        boolean isCurrentUser = userId.equals(followerId);

        return createFollowDto(follow, followerId, isFollowingType, userId, nickname, profile, isCurrentUser);
    }

    private FollowDto createFollowDto(Follow follow, Long followerId, boolean isFollowingType, Long userId, String nickname,
                                      String profile, boolean isCurrentUser) {
        return FollowDto.of(
                follow.getId(),
                userId,
                nickname,
                profile,
                isCurrentUser ? null : checkFollow(follow, followerId, isFollowingType ? FOLLOWING : null),
                isCurrentUser ? true : null
        );
    }

    /**
     * 팔로워 목록에서 현재 사용자가 조회된사용자를 팔로워 하는지 체크
     */
    private boolean checkFollow(Follow follow, Long followerId, String type) {
        BooleanBuilder builder = checkFollowBuilder(follow, followerId, type);
        FollowStatus followStatus = followStatusQuery(builder);
        return followStatus != null && (followStatus.equals(FollowStatus.FOLLOWING) || followStatus.equals(FollowStatus.MUTUAL_FOLLOW));
    }

    private BooleanBuilder checkFollowBuilder(Follow follow, Long followerId, String type) {
        BooleanBuilder builder = new BooleanBuilder();
        if (type.equals(FOLLOWING)) {
            return builder.and(QFollow.follow.follower.id.eq(followerId)
                    .and(QFollow.follow.following.id.eq(follow.getFollowing().getId())));
        }
        return builder.and(QFollow.follow.following.id.eq(follow.getFollower().getId())
                .and(QFollow.follow.follower.id.eq(followerId)));
    }

    private FollowStatus followStatusQuery(BooleanBuilder builder) {
        return jpaQueryFactory.select(QFollow.follow.status)
                .from(QFollow.follow)
                .where(builder).fetchOne();
    }

    private List<Follow> getFollowersQuery(Long followId, Pageable pageable, BooleanBuilder builder) {
        return jpaQueryFactory.select(follow)
                .from(follow)
                .join(follow.follower, member).fetchJoin()
                .where(builder, follow.following.id.eq(followId).and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW)))
                        .and(follow.follower.isWithdrawn.eq(false)))
                .limit(pageable.getPageSize() + 1)
                .orderBy(follow.id.asc())
                .fetch();
    }

    private List<FollowDto> getFollowerDtos(Long followerId, Long followId, List<Follow> followList) {
        if (!followerId.equals(followId)) {       // 상대방 계정 조회시
            if (!followList.isEmpty()) {
                validPrivateAccount(followerId, followId, followList.get(0).getFollowing().getAccountStatus());  // 해당 사용자가 팔로우 상태인지 확인
                return getFriendFollows(followerId, followList, FOLLOWERS);
            }
        }
        return getMyFollows(followList, FOLLOWERS);

    }

    /*
    현재 조회할 사용자가 비공개 계정일때 맞팔로우 상태가 아니라면 예외 발생
     */
    private void validPrivateAccount(Long followerId, Long followId, AccountStatus accountStatus) {
        FollowStatus followStatus = jpaQueryFactory.select(follow.status)
                .from(follow)
                .where(follow.following.id.eq(followerId).and(follow.follower.id.eq(followId))).fetchOne();
        if (!followStatus.equals(FollowStatus.MUTUAL_FOLLOW) && accountStatus.equals(AccountStatus.PRIVATE))
            throw new InvalidUsageException(ExceptionStatus.BAD_REQUEST, ExceptionType.PRIVATE_ACCOUNT);
    }

    /**
     * 현재 사용자의 팔로워/팔로잉 목록을조회합니다.
     */
    private List<FollowDto> getMyFollows(List<Follow> followList, String type) {
        return followList.stream()
                .map(follow -> FollowDto.of(
                        follow.getId(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getId() : follow.getFollower().getId(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getNickname() : follow.getFollower().getNickname(),
                        type.equals(FOLLOWING) ? follow.getFollowing().getProfile() : follow.getFollower().getProfile(),
                        checkFollowing(follow.getStatus()))
                )
                .collect(Collectors.toList());
    }

    private static boolean checkFollowing(FollowStatus followStatus) {
        return followStatus.equals(FollowStatus.FOLLOWING) || followStatus.equals(FollowStatus.MUTUAL_FOLLOW);
    }

    private static long countNull(Long count) {
        return count == null ? 0 : count;
    }

    private FollowStatus followStatusQuery(Long followerId, Long followingId) {
        return jpaQueryFactory.select(follow.status)
                .from(follow)
                .where(follow.follower.id.eq(followerId)
                        .and(follow.following.id.eq(followingId)))
                .fetchOne();
    }

    private FollowType handleFollowStatus(FollowStatus userToTargetStatus, FollowStatus targetToUserStatus) {
        if (isNotFollowing(userToTargetStatus)) { // 사용자가 상대방을 팔로우하지 않는 경우
            if (isNotFollowing(targetToUserStatus)) {
                return FollowType.NEITHER;
            } else if (targetToUserStatus == FollowStatus.FOLLOWING) {
                return FollowType.FOLLOW_ONLY;
            }
        } else if (isFollowingOrMutual(userToTargetStatus)) {
            return FollowType.FOLLOW;
        } else if (userToTargetStatus == FollowStatus.REQUEST) {
            return FollowType.FOLLOW_ONLY;
        }
        return FollowType.NEITHER;
    }

    private boolean isNotFollowing(FollowStatus status) {
        return status == null || status == FollowStatus.CANCEL || status == FollowStatus.UNFOLLOW;
    }

    private boolean isFollowingOrMutual(FollowStatus status) {
        return status == FollowStatus.FOLLOWING || status == FollowStatus.MUTUAL_FOLLOW;
    }

    private static boolean isHasNext(Pageable pageable, List<FollowDto> followingDtoList) {
        boolean hasNext = false;
        if (followingDtoList.size() > pageable.getPageSize()) {
            hasNext = true;
            followingDtoList.remove(pageable.getPageSize());
        }
        return hasNext;
    }
}
