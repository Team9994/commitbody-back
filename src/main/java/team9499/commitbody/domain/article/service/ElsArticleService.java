package team9499.commitbody.domain.article.service;

import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.dto.response.AllArticleResponse;

public interface ElsArticleService {

    void saveArticleAsync(ArticleDto articleDto);

    void updateArticleAsync(ArticleDto articleDto);

    void deleteArticleAsync(Long articleId);

    AllArticleResponse searchArticleByTitle(Long memberId, String title, ArticleCategory category,Integer size, Long lastId);

    void updateWriterAsync(String beforeNickname, String afterNickname);

    void updateArticleCountAsync(Long articleId, Integer count,String type);

    void updateArticleWithDrawAsync(Long memberId, Boolean type);

    void updateArticleLikeAndCommentCountAsync(Long memberId, Boolean type);
}
