package team9499.commitbody.domain.article.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.global.utils.TimeConverter;

@Data
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleDto {

    private Long articleId;     // 게시글 ID

    private String title;       // 게시글 제목

    private ArticleCategory articleCategory;        // 게시글 타입

    private String time;        // 작성 시간

    private Integer likeCount;      // 좋아요 수

    private Integer commentCount;  // 게시글 수

    private String imageUrl;        // 이미지 url


    public static ArticleDto of(Long articleId, String imageUrl){
        return ArticleDto.builder().articleId(articleId).imageUrl(imageUrl).build();
    }

    public static ArticleDto of(Article article, String imageUrl){
        return ArticleDto.builder().articleId(article.getId()).title(article.getTitle()).articleCategory(article.getArticleCategory()).time(TimeConverter.converter(article.getCreatedAt())).likeCount(article.getLikeCount())
                .commentCount(article.getCommentCount()).imageUrl(imageUrl).build();
    }

}
