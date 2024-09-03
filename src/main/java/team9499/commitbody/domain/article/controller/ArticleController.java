package team9499.commitbody.domain.article.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.article.dto.request.ArticleSaveRequest;
import team9499.commitbody.domain.article.service.ArticleService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping(value = "/article",consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveArticle(@Valid @RequestPart("articleSaveRequest") ArticleSaveRequest request, BindingResult result,
                                         @RequestPart("file")MultipartFile file,
                                         @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        Long articleId = articleService.saveArticle(memberId, request.getTitle(), request.getContent(), request.getArticleType(), request.getArticleCategory(), request.getVisibility(), file);
        return ResponseEntity.ok(new SuccessResponse<>(true,"둥록 성공",articleId));
    }
}
