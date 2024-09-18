package team9499.commitbody.domain.article.service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.dto.response.AllArticleResponse;
import team9499.commitbody.domain.article.dto.response.ProfileArticleResponse;

public interface ArticleService {

    AllArticleResponse getAllArticles(Long memberId,ArticleType type, ArticleCategory articleCategory,Long lastId,Pageable pageable);

    Long saveArticle(Long memberId, String title, String content, ArticleType articleType, ArticleCategory articleCategory, Visibility visibility, MultipartFile file);

    ArticleDto getDetailArticle(Long memberId, Long articleId);

    ProfileArticleResponse getAllProfileArticle(Long loginMemberId, Long findMemberId, ArticleType articleType, Long lastId, Pageable pageable);

    void updateArticle(Long memberId, Long articleId, String title, String content, ArticleType articleType, ArticleCategory articleCategory, Visibility visibility, MultipartFile file);

    void deleteArticle(Long memberId, Long articleId);
}
