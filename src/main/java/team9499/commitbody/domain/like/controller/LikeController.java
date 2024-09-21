package team9499.commitbody.domain.like.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;
import team9499.commitbody.domain.article.event.ElsArticleCountEvent;
import team9499.commitbody.domain.like.service.LikeService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LikeController {

    private final LikeService exerciseCommentLikeService;
    private final ApplicationEventPublisher eventPublisher;

    @Operation(summary = "운동 댓글 좋아요", description = "운동 상세 페이지의 작성된 댓글을 좋아요/취소가 가능합니다. ",tags = "운동 상세")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200_1", description = "댓글 등록시", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"등록\"}"))),
            @ApiResponse(responseCode = "200_2", description = "댓글 취소시", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"해제\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/comment-exercise/like")
    public ResponseEntity<?> exerciseLike(@Parameter(schema = @Schema(example = "{\"exCommentId\":1}")) @RequestBody Map<String, Long> exCommentLikeRequest,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        String commentLikeStatus = exerciseCommentLikeService.updateCommentLike(exCommentLikeRequest.get("exCommentId"), memberId);
        return ResponseEntity.ok(new SuccessResponse<>(true,commentLikeStatus));
    }

    @Operation(summary = "게시글 좋아요", description = "게시글의 대해서 좋아요 등록/해제 기능을 합니다.",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - 좋아요 성공시[\"등록\"], 좋아요 해제시[\"해제\"]", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"등록\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 존재하지 않는 게시글 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/article/like")
    public ResponseEntity<?> articleLike(@Parameter(schema = @Schema(example = "{\"articleId\": 1}"))@RequestBody Map<String, Long> articleLikeRequest,
                                         @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        ArticleCountResponse response = exerciseCommentLikeService.articleLike(articleLikeRequest.get("articleId"), memberId);

        eventPublisher.publishEvent(new ElsArticleCountEvent(response.getArticleId(),response.getCount(),"좋아요"));

        return ResponseEntity.ok(new SuccessResponse<>(true,response.getType()));
    }

    @Operation(summary = "게시글 대/댓글 좋아요", description = "게시글의 작성된 대/댓글의 대해서 등록/해제 기능을 합니다.",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - 좋아요 성공시[\"등록\"], 좋아요 해제시[\"해제\"]", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"등록\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 존재하지 않는 게시글 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/article/comment/like")
    public ResponseEntity<?> articleCommentLike(@Parameter(schema = @Schema(example = "{\"commentId\": 1}"))@RequestBody Map<String, Long> articleLikeRequest,
                                         @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        String articleLike = exerciseCommentLikeService.articleCommentLike(memberId,articleLikeRequest.get("commentId"));
        return ResponseEntity.ok(new SuccessResponse<>(true,articleLike));
    }
}
