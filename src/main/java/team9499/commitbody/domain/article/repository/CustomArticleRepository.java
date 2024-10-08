package team9499.commitbody.domain.article.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.dto.ArticleDto;

import java.util.Map;

public interface CustomArticleRepository {

    Slice<ArticleDto> getAllArticles(Long memberId, ArticleType type, ArticleCategory articleCategory, Long lastId, Pageable pageable);

    Slice<ArticleDto> getAllProfileArticle(Long loginMemberId, Long findMemberId, boolean myAccount, ArticleType articleType, Long lastId, Pageable pageable);

    ArticleDto getDetailArticle(Long memberId, Long articleId);

    Map<String, Object> getArticleAndFile(Long articleId);

    void deleteArticle(Long articleId);
}
