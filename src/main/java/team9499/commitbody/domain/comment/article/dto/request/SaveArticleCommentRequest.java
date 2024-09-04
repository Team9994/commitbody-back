package team9499.commitbody.domain.comment.article.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "게시글 댓글 작성 Request")
@Data
public class SaveArticleCommentRequest {

    @Schema(description = "게시글 ID")
    private Long articleId;     // 게시글 Id

    @Schema(description = "댓글을단 사용자의 닉네임(댓글의 답글을 작성시에만 사용)")
    private String replyNickname;   // 답글 일경우 대상자 이름

    @Schema(description = "댓글의 답급을 작성시 댓글ID를 작성")
    private Long parentId;      // 부모 댓글 Id

    @Schema(description = "댓글의 내용")
    @NotBlank(message = "댓글 내용을 입력해주세요")
    private String content;     // 댓글 내용
}
