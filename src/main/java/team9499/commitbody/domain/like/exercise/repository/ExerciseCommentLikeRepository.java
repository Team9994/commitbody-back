package team9499.commitbody.domain.like.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.like.exercise.domain.ExerciseCommentLike;

import java.util.Optional;

@Repository
public interface ExerciseCommentLikeRepository extends JpaRepository<ExerciseCommentLike, Long>{

    Optional<ExerciseCommentLike> findByMemberIdAndExerciseCommentId(Long memberId, Long exCommentId);

}
