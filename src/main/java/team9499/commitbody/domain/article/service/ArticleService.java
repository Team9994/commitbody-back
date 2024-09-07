package team9499.commitbody.domain.article.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
import team9499.commitbody.domain.article.dto.response.ExerciseArticleResponse;

public interface ArticleService {

    Long saveArticle(Long memberId, String title, String content, ArticleType articleType, ArticleCategory articleCategory, Visibility visibility, MultipartFile file);

    ExerciseArticleResponse getAllExerciseArticle(String loginNickname, String findNickname, Long lastId, Pageable pageable);
}
