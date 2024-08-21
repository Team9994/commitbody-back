package team9499.commitbody.domain.comment.exercise.service;

import org.springframework.data.domain.Pageable;
import team9499.commitbody.domain.comment.exercise.dto.response.ExerciseCommentResponse;

public interface ExerciseCommentService {

    void saveExerciseComment(Long memberId, Long exerciseId, String source,String comment);

    ExerciseCommentResponse getExerciseComments(Long memberId, Long exerciseId, String source, Pageable pageable, Long lastId);
}
