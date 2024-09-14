package team9499.commitbody.domain.article.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.dto.ArticleDto;

import java.util.Map;

public interface CustomArticleRepository {

    Slice<ArticleDto> getAllProfileArticle(Long loginMemberId, Long findMemberId, boolean myAccount, ArticleType articleType, Long lastId, Pageable pageable);

    ArticleDto getDetailArticle(Long memberId, Long articleId);

    Map<String, Object> getArticleAndFile(Long articleId);
}
