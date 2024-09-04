package team9499.commitbody.domain.comment.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;

@Repository
public interface ArticleCommentRepository extends JpaRepository<ArticleComment,Long>{
}
