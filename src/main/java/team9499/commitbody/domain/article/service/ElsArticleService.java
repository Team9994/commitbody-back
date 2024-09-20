package team9499.commitbody.domain.article.service;

import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.dto.response.AllArticleResponse;

public interface ElsArticleService {

    void saveArticle(ArticleDto articleDto);

    AllArticleResponse searchArticleByTitle(Long memberId, String title, ArticleCategory category,Integer size, Long lastId);

    void updateWriterAsync(String beforeNickname, String afterNickname);
}