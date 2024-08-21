package team9499.commitbody.domain.like.exercise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.like.exercise.service.ExerciseCommentLikeService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.SuccessResponse;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ExerciseCommentLikeController {

    private final ExerciseCommentLikeService exerciseCommentLikeService;

    @PostMapping("/comment-exercise/like")
    public ResponseEntity<?> exerciseLike(@RequestBody Map<String, Long> exCommentLikeRequest,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        String commentLikeStatus = exerciseCommentLikeService.updateCommentLike(exCommentLikeRequest.get("exCommentId"), memberId);
        return ResponseEntity.ok(new SuccessResponse<>(true,commentLikeStatus));
    }

}
