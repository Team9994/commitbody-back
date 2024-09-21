package team9499.commitbody.domain.article.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ElsArticleCountEvent {

    private Long articleId;

    private Integer count;

    private String type;
}
