package team9499.commitbody.domain.comment.article.service;

import org.springframework.data.domain.Pageable;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.response.ArticleCommentResponse;

public interface ArticleCommentService {
    String saveArticleComment(Long memberId, Long ArticleId,Long commentParentId,String content,String replyNickname);
    ArticleCommentResponse getComments(Long articleId, Long memberId, Long lastId,Integer lastLikeCount,OrderType orderType , Pageable pageable);
}
