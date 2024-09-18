package team9499.commitbody.domain.article.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.article.dto.ArticleDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AllArticleResponse {

    private boolean hasNext;

    private List<ArticleDto> articles;
}
