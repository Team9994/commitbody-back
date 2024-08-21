package team9499.commitbody.domain.comment.exercise.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team9499.commitbody.domain.comment.exercise.service.ExerciseCommentService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.SuccessResponse;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ExerciseCommentController {

    private final ExerciseCommentService exerciseCommentService;

    @PostMapping("/exercise-comment")
    public ResponseEntity<?> saveExerciseComment(@RequestBody Map<String,String> commentRequest,
                                                 @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        String content = commentRequest.get("content");
        exerciseCommentService.saveExerciseComment(memberId,content);
        return ResponseEntity.ok(new SuccessResponse<>(true,"등록 성공"));
    }
}
