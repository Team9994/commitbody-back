package team9499.commitbody.domain.comment.article.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleCommentResponse {

    private Integer totalCount;

    private boolean hasNext;

    private List<ArticleCommentDto> comments;
}
