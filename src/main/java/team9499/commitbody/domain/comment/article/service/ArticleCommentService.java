package team9499.commitbody.domain.comment.article.service;

import org.springframework.data.domain.Pageable;
import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.response.ArticleCommentResponse;

import java.util.List;


public interface ArticleCommentService {

    ArticleCountResponse saveArticleComment(Long memberId, Long ArticleId, Long commentParentId, String content, String replyNickname);

    void updateArticleComment(Long memberId, Long commentId,String content);

    ArticleCommentResponse getComments(Long articleId, Long memberId, Long lastId, Integer lastLikeCount, OrderType orderType, Pageable pageable);

    ArticleCommentResponse getReplyComments(Long commentId, Long memberId,Long lastId,Pageable pageable);

    ArticleCountResponse deleteArticleComment(Long memberId, Long commentId);

    List<Long> getWriteDrawArticleIdsByComment(Long memberId);

}
