package team9499.commitbody.global.batch.mapper;

import org.springframework.jdbc.core.RowMapper;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomCommentRowMapper implements RowMapper<ArticleComment> {
    @Override
    public ArticleComment mapRow(ResultSet rs, int rowNum) throws SQLException {
        return ArticleComment.builder()
               .article(Article.builder().id(rs.getLong("article_id")).build()).build();
    }
}
