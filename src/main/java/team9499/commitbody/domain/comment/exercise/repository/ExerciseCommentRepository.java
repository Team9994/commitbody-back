package team9499.commitbody.domain.comment.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;

@Repository
public interface ExerciseCommentRepository extends JpaRepository<ExerciseComment, Long>, CustomExerciseCommentRepository {

    void deleteByMemberIdAndId(Long memberId, Long exerciseCommentId);
}
