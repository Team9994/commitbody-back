package team9499.commitbody.domain.comment.exercise.dto;

import lombok.Data;

@Data
public class ExerciseCommentRequest {

    private Long exerciseId;
    private String content;
    private String source;
}
