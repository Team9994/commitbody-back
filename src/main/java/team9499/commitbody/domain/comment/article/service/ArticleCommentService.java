package team9499.commitbody.domain.comment.article.service;

import org.springframework.data.domain.Pageable;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.response.ArticleCommentCountResponse;
import team9499.commitbody.domain.comment.article.dto.response.ArticleCommentResponse;


public interface ArticleCommentService {

    ArticleCommentCountResponse saveArticleComment(Long memberId, Long ArticleId, Long commentParentId, String content, String replyNickname);

    void updateArticleComment(Long memberId, Long commentId,String content);

    ArticleCommentResponse getComments(Long articleId, Long memberId, Long lastId, Integer lastLikeCount, OrderType orderType, Pageable pageable);

    ArticleCommentResponse getReplyComments(Long commentId, Long memberId,Long lastId,Pageable pageable);

    ArticleCommentCountResponse deleteArticleComment(Long memberId, Long commentId);

}
