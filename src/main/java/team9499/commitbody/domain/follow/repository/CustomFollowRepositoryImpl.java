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
import team9499.commitbody.domain.follow.domain.Follow;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.follow.dto.FollowerDto;
import team9499.commitbody.domain.follow.dto.FollowingDto;

import java.util.List;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.Member.domain.QMember.*;
import static team9499.commitbody.domain.follow.domain.QFollow.*;

@Repository
@RequiredArgsConstructor
public class CustomFollowRepositoryImpl implements CustomFollowRepository{


    private final JPAQueryFactory jpaQueryFactory;

    private final String FULL_TEXT_INDEX ="function('match',{0},{1})";

    /**
     * 해당 사용자가 팔로잉한 사용자의 목록을 조회합니다.
     * @param followerId 조회할 사용자
     */
    @Override
    public Slice<FollowingDto> getAllFollowings(Long followerId, String nickName,Long lastId, Pageable pageable) {
        BooleanBuilder builder = lastIdBuilder(lastId);
        nicknameBuilder(nickName, builder);

        List<Follow> followList = jpaQueryFactory.select(follow)
                .from(follow)
                .join(member).on(member.id.eq(follow.following.id))
                .where(builder,follow.follower.id.eq(followerId).and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW))))
                .limit(pageable.getPageSize()+1)
                .orderBy(follow.id.asc())
                .fetch();

        List<FollowingDto> followingDtoList = followList.stream()
                .map(follow -> new FollowingDto(
                        follow.getId(),follow.getFollowing().getId(), follow.getFollowing().getNickname(),follow.getFollowing().getProfile())
                )
                .collect(Collectors.toList());

        boolean hasNext = false;
        if (followingDtoList.size() > pageable.getPageSize()){
            hasNext = true;
            followingDtoList.remove(pageable.getPageSize());
        }
        return new SliceImpl<>(followingDtoList,pageable,hasNext);
    }

    /**
     * 해당 사용자를 팔로워 하는 목록을 조회
     * @param followerId 조회할 사용자
     */
    @Override
    public Slice<FollowerDto> getAllFollowers(Long followerId, String nickName, Long lastId, Pageable pageable) {
        BooleanBuilder builder = lastIdBuilder(lastId);
        nicknameBuilder(nickName,builder);

        List<Follow> followList = jpaQueryFactory.select(follow)
                .from(follow)
                .join(member).on(member.id.eq(follow.follower.id))  // 올바른 조인 조건
                .where(builder,follow.following.id.eq(followerId).and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW))))  // 올바른 조건
                .orderBy(follow.id.asc())
                .fetch();

        List<FollowerDto> followerDtoList = followList.stream()
                .map(follow -> new FollowerDto(
                        follow.getId(), follow.getFollower().getId(), follow.getFollower().getNickname(), follow.getFollower().getProfile(),checkFollow(follow.getStatus()))
                )
                .collect(Collectors.toList());

        boolean hasNext = false;
        if (followerDtoList.size() > pageable.getPageSize()){
            hasNext= true;
            followerDtoList.remove(pageable.getPageSize());
        }
        return new SliceImpl<>(followerDtoList,pageable,hasNext);
    }

    private static boolean checkFollow(FollowStatus followStatus){
        return followStatus.equals(FollowStatus.FOLLOWING) ? false : true;
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
}
