package team9499.commitbody.domain.article.service;

import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;

public interface ArticleService {

    Long saveArticle(Long memberId, String title, String content, ArticleType articleType, ArticleCategory articleCategory, Visibility visibility, MultipartFile file);
}
