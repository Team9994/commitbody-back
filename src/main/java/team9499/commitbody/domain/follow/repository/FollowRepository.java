package team9499.commitbody.domain.follow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.follow.domain.Follow;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long>, CustomFollowRepository {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId,Long followingId);
}
