package team9499.commitbody.domain.comment.article.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;

public interface CustomArticleRepository {
    Slice<ArticleCommentDto> getAllCommentByArticle(Long articleId, Long memberId, Long lastId,Integer lastLikeCount,OrderType orderType, Pageable pageable);
}
