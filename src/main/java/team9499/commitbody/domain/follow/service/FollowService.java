package team9499.commitbody.domain.follow.service;

import org.springframework.data.domain.Pageable;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.domain.follow.dto.response.FollowResponse;

public interface FollowService {

    String follow(Long followerId, Long followingId, FollowType type);

    FollowResponse getFollowers(Long followingId, Long followerId,String nickName, Long lastId, Pageable pageable);

    FollowResponse getFollowings(Long followerId,Long followingId ,String nickName, Long lastId, Pageable pageable);

    void cancelFollow(Long followerId, Long followingId);
}
