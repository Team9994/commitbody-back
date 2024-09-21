package team9499.commitbody.domain.article.dto.response;

import lombok.*;
@Data
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ArticleCountResponse {

    private Long articleId;

    private Integer count;

    private String type;

    public static ArticleCountResponse of(Long articleId, Integer count, String type){
        ArticleCountResponseBuilder builder = ArticleCountResponse.builder().articleId(articleId).count(count);
        if (type != null){
            builder.type(type);
        }
        return builder.build();
    }
}
