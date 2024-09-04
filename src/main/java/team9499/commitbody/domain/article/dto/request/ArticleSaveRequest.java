package team9499.commitbody.domain.article.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
import team9499.commitbody.global.annotations.ValidEnum;

@Schema(description = "운동 게시글 작성 Request")
@Data
public class ArticleSaveRequest {

    @Schema(description = "게시글 제목")
    @NotBlank(message = "게시글 제목을 작성해주세요")
    private String title;
    
    @Schema(description = "게시글 내용 작성")
    @NotBlank(message = "게시글 내용을 작성해 주세요")
    private String content;

    @Schema(description = "게시글 종류 [EXERCISE(운동 인증), INFO_QUESTION(정보 질문)] 을 사용합니다.")
    @ValidEnum(message = "게시글 종류를 작성해주세요",enumClass = ArticleType.class)
    private ArticleType articleType;

    @Schema(description = "게시글 종류가 INFO_QUESTION일 경우 사용합니다.")
    private ArticleCategory articleCategory;

    @Schema(description = "게시글 작성시 게시글 공개범위를 선택합니다.[PUBLIC(전체 공개), FOLLOWERS_ONLY(팔로워만 공개), PRIVATE(비공개)]")
    private Visibility visibility;
}
