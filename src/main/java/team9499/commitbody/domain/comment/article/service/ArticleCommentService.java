package team9499.commitbody.domain.comment.article.service;

public interface ArticleCommentService {
    String saveArticleComment(Long memberId, Long ArticleId,Long commentParentId,String content,String replyNickname);
}
