package team9499.commitbody.domain.comment.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.comment.exercise.dto.ExerciseCommentRequest;
import team9499.commitbody.domain.comment.exercise.dto.response.ExerciseCommentResponse;
import team9499.commitbody.domain.comment.exercise.service.ExerciseCommentService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ExerciseCommentController {

    private final ExerciseCommentService exerciseCommentService;

    @Operation(summary = "운동 댓글 등록", description = "사용자는 해당 운동 목록에 댓글을 등록 가능합니다.",tags = "운동 상세")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"등록 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/comment-exercise")
    public ResponseEntity<?> saveExerciseComment(@RequestBody ExerciseCommentRequest exerciseCommentRequest,
                                                 @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();

        exerciseCommentService.saveExerciseComment(memberId,exerciseCommentRequest.getExerciseId(), exerciseCommentRequest.getSource(), exerciseCommentRequest.getContent());
        return ResponseEntity.ok(new SuccessResponse<>(true,"등록 성공"));
    }

    @GetMapping("/comment-exercise/{id}")
    public ResponseEntity<?> getExerciseComment(@PathVariable("id") Long exerciseId,
                                                @RequestParam("source") String source,
                                                @RequestParam(name = "lastId",required = false) Long lastId,
                                                @AuthenticationPrincipal PrincipalDetails principalDetails,
                                                @PageableDefault Pageable pageable){
        Long memberId = principalDetails.getMember().getId();
        ExerciseCommentResponse exerciseComments = exerciseCommentService.getExerciseComments(memberId, exerciseId, source, pageable, lastId);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",exerciseComments));
    }
}
