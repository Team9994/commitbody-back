package team9499.commitbody.domain.follow.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.domain.follow.dto.FollowerDto;
import team9499.commitbody.domain.follow.dto.FollowingDto;

import java.util.List;

public interface CustomFollowRepository {

    Slice<FollowingDto> getAllFollowings(Long followerId, String nickName, Long lastId, Pageable pageable);

    Slice<FollowerDto> getAllFollowers(Long followerId,String nickName,Long lastId, Pageable pageable);
}