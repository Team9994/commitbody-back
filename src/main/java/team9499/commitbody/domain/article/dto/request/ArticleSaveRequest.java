package team9499.commitbody.domain.article.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
import team9499.commitbody.global.annotations.ValidEnum;

@Data
public class ArticleSaveRequest {

    @NotBlank(message = "게시글 제목을 작성해주세요")
    private String title;

    @NotBlank(message = "게시글 내용을 작성해 주세요")
    private String content;

    @ValidEnum(message = "게시글 종류를 작성해주세요",enumClass = ArticleType.class)
    private ArticleType articleType;
    
    private ArticleCategory articleCategory;

    private Visibility visibility;
}
