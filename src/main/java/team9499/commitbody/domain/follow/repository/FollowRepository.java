package team9499.commitbody.domain.follow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.follow.domain.Follow;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long>, CustomFollowRepository {

    @Query("select f from Follow f where f.follower.id = :followerId and f.following.id = :followingId and f.status = 'REQUEST'")
    Optional<Follow> findByFollowReqeust(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Query("select f from Follow f where f.follower.id = :followerId and f.following.id = :followingId and f.status = 'FOLLOWING'")
    Optional<Follow> findByFollowerReqeust(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId,Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId,Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId,Long followingId);

}
