package team9499.commitbody.domain.comment.article.dto.request;

import lombok.Data;

@Data
public class SaveArticleCommentRequest {

    private Long articleId;     // 게시글 Id

    private String replyNickname;   // 답글 일경우 대상자 이름

    private Long parentId;      // 부모 댓글 Id

    private String content;     // 댓글 내용
}
