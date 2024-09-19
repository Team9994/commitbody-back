package team9499.commitbody.domain.article.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.article.dto.ArticleDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllArticleResponse {

    private Integer totalCount;
    private boolean hasNext;

    private List<ArticleDto> articles;
}
