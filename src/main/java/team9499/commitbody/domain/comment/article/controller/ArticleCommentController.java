package team9499.commitbody.domain.comment.article.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.comment.article.dto.request.SaveArticleCommentRequest;
import team9499.commitbody.domain.comment.article.service.ArticleCommentService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.SuccessResponse;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ArticleCommentController {

    private final ArticleCommentService articleCommentService;

    @PostMapping("/article/comment")
    public ResponseEntity<?> save(@RequestBody SaveArticleCommentRequest request,
                                  @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        String commentType = articleCommentService.saveArticleComment(memberId, request.getArticleId(), request.getParentId(), request.getContent(), request.getReplyNickname());

        return ResponseEntity.ok(new SuccessResponse<>(true,commentType));
    }

    private static Long getMemberId(PrincipalDetails principalDetails) {
        Long memberId = principalDetails.getMember().getId();
        return memberId;
    }
}
