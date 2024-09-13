package team9499.commitbody.domain.comment.article.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;

import java.util.List;


public interface CustomArticleCommentRepository {

    Slice<ArticleCommentDto> getAllCommentByArticle(Long articleId, Long memberId, Long lastId,Integer lastLikeCount,OrderType orderType, Pageable pageable);

    Integer getCommentCount(Long articleId, Long memberId);

    Slice<ArticleCommentDto> getAllReplyComments(Long commentId, Long memberId,Long lastId ,Pageable pageable);

    List<Long> getAllChildComment(Long commentId);
}
