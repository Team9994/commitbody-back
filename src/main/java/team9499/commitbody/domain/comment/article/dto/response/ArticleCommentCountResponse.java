package team9499.commitbody.domain.comment.article.dto.response;

import lombok.*;

@Data
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ArticleCommentCountResponse {

    private Long articleId;

    private Integer count;

    private String type;

    public static ArticleCommentCountResponse of(Long articleId,Integer count, String type){
        ArticleCommentCountResponseBuilder builder = ArticleCommentCountResponse.builder().articleId(articleId).count(count);
        if (type != null){
            builder.type(type);
        }
        return builder.build();
    }
}
