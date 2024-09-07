package team9499.commitbody.domain.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.article.dto.request.ArticleSaveRequest;
import team9499.commitbody.domain.article.dto.response.ExerciseArticleResponse;
import team9499.commitbody.domain.article.service.ArticleService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

@Tag(name = "게시글",description = "게시글 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "운동 게시글 등록", description = "운동 게시글을 작성하는 API 입니다. 관심운동 등록시에는 'articleCategory' 필드를 사용하지 않아도 됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - 게시글 작성 성공시 작성된 게시글 ID를 반환합니다.", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"등록 성공\",\"data\":1}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 파일 용량 초과(5MB 이하만 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"저장 가능한 용량을 초과 했습니다.\"}"))),
            @ApiResponse(responseCode = "400_4", description = "BADREQUEST - 불가능한 이미지 파일 저장시(jpeg, jpg, png, gif 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"올바른 이미지 형식이 아닙니다.\"}"))),
            @ApiResponse(responseCode = "400_5", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping(value = "/article",consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveArticle(@Valid @RequestPart("articleSaveRequest") ArticleSaveRequest request, BindingResult result,
                                         @RequestPart("file")MultipartFile file,
                                         @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        Long articleId = articleService.saveArticle(memberId, request.getTitle(), request.getContent(), request.getArticleType(), request.getArticleCategory(), request.getVisibility(), file);
        return ResponseEntity.ok(new SuccessResponse<>(true,"둥록 성공",articleId));
    }

    @GetMapping("/my-page/exercise/{nickname}")
    public ResponseEntity<?> getAllExerciseArticle(@PathVariable("nickname") String nickname,
                                                   @RequestParam(value = "lastId",required = false) Long lastId,
                                                   @AuthenticationPrincipal PrincipalDetails principalDetails,
                                                   @PageableDefault(size = 12) Pageable pageable){
        String loginNickname = principalDetails.getMember().getNickname();
        ExerciseArticleResponse articles = articleService.getAllExerciseArticle(loginNickname, nickname, lastId, pageable);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",articles));
    }

    private static Long getMemberId(PrincipalDetails principalDetails) {
        Long memberId = principalDetails.getMember().getId();
        return memberId;
    }
}
