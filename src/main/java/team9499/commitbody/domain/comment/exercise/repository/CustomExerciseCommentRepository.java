package team9499.commitbody.domain.comment.exercise.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.domain.comment.exercise.dto.ExerciseCommentDto;

public interface CustomExerciseCommentRepository {

    Slice<ExerciseCommentDto> getExerciseComments(Long memberId, Long exerciseId, String source, Pageable pageable, Long lastId);
}
