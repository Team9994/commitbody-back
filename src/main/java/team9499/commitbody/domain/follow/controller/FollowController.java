package team9499.commitbody.domain.follow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.follow.dto.request.FollowRequest;
import team9499.commitbody.domain.follow.dto.response.FollowResponse;
import team9499.commitbody.domain.follow.service.FollowService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FollowController {

    private final FollowService followService;

    @PostMapping("/follow")
    public ResponseEntity<?> requestFollow(@RequestBody FollowRequest followRequest,
                                           @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long followerId = getMember(principalDetails);
        String followStatus = followService.follow(followerId, followRequest.getFollowId(), followRequest.getType());
        return ResponseEntity.ok(new SuccessResponse<>(true,followStatus));
    }
    @GetMapping("/followers")
    public ResponseEntity<?> getFollowers(@RequestParam(value = "lastId",required = false) Long lastId,
                                          @RequestParam(value = "nickname",required = false) String nickname,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails,
                                          @PageableDefault Pageable pageable){

        Long followId = getMember(principalDetails);
        FollowResponse followers = followService.getFollowers(followId,nickname,lastId,pageable);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",followers));
    }

    @GetMapping("/followings")
    public ResponseEntity<?> getFollows(@RequestParam(value = "lastId",required = false) Long lastId,
                                        @RequestParam(value = "nickname",required = false) String nickname,
                                        @AuthenticationPrincipal PrincipalDetails principalDetails,
                                        @PageableDefault Pageable pageable){
        Long followId = getMember(principalDetails);
        FollowResponse followings = followService.getFollowings(followId, nickname, lastId, pageable);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공", followings));
    }

    private static Long getMember(PrincipalDetails principalDetails) {
        Long followId = principalDetails.getMember().getId();
        return followId;
    }
}
