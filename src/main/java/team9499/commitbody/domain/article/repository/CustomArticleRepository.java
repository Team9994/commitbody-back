package team9499.commitbody.domain.article.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.domain.article.dto.ArticleDto;

public interface CustomArticleRepository {

    Slice<ArticleDto> getAllExerciseArticle(String loginNickname, String findNickname, Long lastId, Pageable pageable);
}
