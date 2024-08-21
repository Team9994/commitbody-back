package team9499.commitbody.domain.comment.exercise.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "댓글 수정 Request")
public class UpdateExerciseCommentRequest {

    @Schema(description = "운동 댓글 Id")
    private Long exerciseCommentId;

    @Schema(description = "수정할 새로운 댓글")
    private String content;
}
