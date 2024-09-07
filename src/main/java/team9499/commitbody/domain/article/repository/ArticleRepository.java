package team9499.commitbody.domain.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.article.domain.Article;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, CustomArticleRepository {
}
