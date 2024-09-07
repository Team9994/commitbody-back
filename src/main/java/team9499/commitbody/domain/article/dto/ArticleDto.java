package team9499.commitbody.domain.article.dto;

import lombok.*;
import team9499.commitbody.domain.article.domain.Article;

@Data
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ArticleDto {

    private Long articleId;

    private String imageUrl;


    public static ArticleDto of(Long articleId, String imageUrl){
        return ArticleDto.builder().articleId(articleId).imageUrl(imageUrl).build();
    }

}
