package team9499.commitbody.domain.comment.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.request.SaveArticleCommentRequest;
import team9499.commitbody.domain.comment.article.dto.response.ArticleCommentResponse;
import team9499.commitbody.domain.comment.article.service.ArticleCommentService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ArticleCommentController {

    private final ArticleCommentService articleCommentService;

    @Operation(summary = "운동 게시글 - 댓글 등록", description = "운동 게시글의 댓글을 작성하는 API 입니다. (replyNickname,parentId) 필요시에만 작성합니다.",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - 댓글 작성시 : 댓글 작성 성공, 대댓글 작성시 : 대댓글 작성 성공", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"댓글 작성 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 데이터 미 존재시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "400_4", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/article/comment")
    public ResponseEntity<?> save(@Valid @RequestBody SaveArticleCommentRequest request, BindingResult result,
                                  @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        String commentType = articleCommentService.saveArticleComment(memberId, request.getArticleId(), request.getParentId(), request.getContent(), request.getReplyNickname());

        return ResponseEntity.ok(new SuccessResponse<>(true,commentType));
    }

    @Operation(summary = "운동 게시글 - 댓글 조회", description = "운동 게시글의 작성된 댓글을 조회합니다. sortOrder:[RECENT,LIKE] 이며, LIKE 정렬시 마지막 조회 데이터의 likeCount가 0일경우 lastLikeCount와 lastId를 사용하며, 0이 아닐시에는 lastLikeCount만 사용합니다.)",tags = "게시글")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"댓글 조회\", \"data\": {\"totalCount\" : 2,\"hasNext\": true, \"comments\": [{\"commentId\": 82, \"content\": \"댓글\", \"nickname\": \"세번쨰닉네임\", \"profile\": \"https://d12ryzjapybmlj.cloudfront.net/default.PNG\", \"time\": \"25분 전\", \"likeCount\": 0, \"replyCount\": 0, \"writer\": false}]}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/article/{articleId}/comment")
    public ResponseEntity<?> all(@PathVariable("articleId") Long articleId,
                                 @RequestParam(name = "lastId",required = false) Long lastId,
                                 @RequestParam(name = "lastLikeCount",required = false) Integer lastLikeCount,
                                 @RequestParam(name = "sortOrder",required = false,defaultValue = "RECENT") OrderType orderType,
                                 @AuthenticationPrincipal PrincipalDetails principalDetails,
                                 @Parameter(example = "{\"size\":10}")@PageableDefault Pageable pageable){
        Long memberId = getMemberId(principalDetails);
        ArticleCommentResponse comments = articleCommentService.getComments(articleId, memberId, lastId, lastLikeCount,orderType, pageable);
        
        return ResponseEntity.ok(new SuccessResponse<>(true,"댓글 조회",comments));
    }

    @GetMapping("/article/comment/{commentId}/reply")
    public ResponseEntity<?> allReplyComments(@PathVariable("commentId") Long commentId,
                                              @AuthenticationPrincipal PrincipalDetails principalDetails,
                                              @PageableDefault Pageable pageable){
        Long memberId = getMemberId(principalDetails);
        ArticleCommentResponse replyComments = articleCommentService.getReplyComments(commentId, memberId, pageable);
        return ResponseEntity.ok(new SuccessResponse<>(true,"대댓글 조회",replyComments));
    }

    private static Long getMemberId(PrincipalDetails principalDetails) {
        Long memberId = principalDetails.getMember().getId();
        return memberId;
    }
}
