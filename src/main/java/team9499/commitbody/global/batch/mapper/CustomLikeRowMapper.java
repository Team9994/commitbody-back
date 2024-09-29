package team9499.commitbody.global.batch.mapper;

import org.springframework.jdbc.core.RowMapper;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.like.domain.ContentLike;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomLikeRowMapper implements RowMapper<ContentLike> {
    @Override
    public ContentLike mapRow(ResultSet rs, int rowNum) throws SQLException {
        return ContentLike.builder()
                .article(Article.builder().id(rs.getLong("article_id")).build()).build();
    }
}
