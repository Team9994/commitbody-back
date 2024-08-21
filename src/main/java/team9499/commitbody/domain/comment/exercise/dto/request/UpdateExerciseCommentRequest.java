package team9499.commitbody.domain.comment.exercise.dto.request;

import lombok.Data;

@Data
public class UpdateExerciseCommentRequest {

    private Long exerciseCommentId;

    private String content;
}
