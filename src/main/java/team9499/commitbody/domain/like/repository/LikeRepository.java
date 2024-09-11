package team9499.commitbody.domain.like.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.like.domain.ContentLike;
import team9499.commitbody.domain.like.repository.querydsl.CustomLikeRepository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<ContentLike, Long>, CustomLikeRepository {

    Optional<ContentLike> findByMemberIdAndExerciseCommentId(Long memberId, Long exCommentId);

    Optional<ContentLike> findByMemberIdAndArticleIdAndExerciseCommentIsNull(Long memberId, Long articleId);

}
