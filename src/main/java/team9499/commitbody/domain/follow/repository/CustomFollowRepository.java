package team9499.commitbody.domain.follow.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.domain.follow.dto.FollowDto;

import java.util.List;

public interface CustomFollowRepository {

    Slice<FollowDto> getAllFollowings(Long followerId, Long followId, String nickName, Long lastId, Pageable pageable);

    Slice<FollowDto> getAllFollowers(Long followerId, Long followId, String nickName, Long lastId, Pageable pageable);

    long getCountFollowing(Long followerId);

    long getCountFollower(Long followingId);

    FollowType followStatus(Long followerId, Long followingId);

    void cancelFollow(Long followerId, Long followingId);

    List<Long> followings(Long followerId);
}
