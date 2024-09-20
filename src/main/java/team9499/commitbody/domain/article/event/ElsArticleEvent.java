package team9499.commitbody.domain.article.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.article.dto.ArticleDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElsArticleEvent {

    private ArticleDto articleDto;

    private String type;
}
