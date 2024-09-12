package team9499.commitbody.domain.comment.article.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleCommentResponse {

    private Integer totalCount;

    private boolean hasNext;

    private List<ArticleCommentDto> comments;

    public ArticleCommentResponse(boolean hasNext, List<ArticleCommentDto> comments) {
        this.hasNext = hasNext;
        this.comments = comments;
    }
}
