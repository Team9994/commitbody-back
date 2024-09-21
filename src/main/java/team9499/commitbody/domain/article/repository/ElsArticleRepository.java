package team9499.commitbody.domain.article.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.article.domain.ArticleDoc;

public interface ElsArticleRepository extends ElasticsearchRepository<ArticleDoc,String> {
}
