package team9499.commitbody.domain.follow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.follow.dto.request.FollowRequest;
import team9499.commitbody.domain.follow.dto.response.FollowResponse;
import team9499.commitbody.domain.follow.service.FollowService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.notification.event.FollowingEvent;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

@Tag(name = "팔로워",description = "팔로워 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FollowController {

    private final FollowService followService;
    private final ApplicationEventPublisher eventPublisher;

    private final String MUTUAL_FOLLOW = "맞팔로우";
    private final String REQUEST_FOLLOW = "팔로우 요청";

    @Operation(summary = "팔로워", description = "하나의 API를 통해 팔로워/팔로잉등 요청을 수행합니다.type의 팔로우요청 , 맞팔로우시 FOLLOW, 언팔시및 팔로우 취소시 UNFOLLOW를 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공시 - 각 요청 응답에 맞게 [맞팔로우,언팔로우,팔로우 요청,팔로우 취소]을 반환 합니다.", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"맞팔로우\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 반복 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"이미 처리된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 사용자 존재하지 않을시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/follow")
    public ResponseEntity<?> requestFollow(@RequestBody FollowRequest followRequest,
                                           @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long followerId = getMember(principalDetails);
        String followStatus = followService.follow(followerId, followRequest.getFollowId(), followRequest.getType());

        if (followStatus.equals(MUTUAL_FOLLOW)||followStatus.equals(REQUEST_FOLLOW))
          eventPublisher.publishEvent(new FollowingEvent(followerId,followRequest.getFollowId()));

        return ResponseEntity.ok(new SuccessResponse<>(true,followStatus));
    }

    @Operation(summary = "팔로워 목록(무한 스크롤)", description = "팔로워 목록을 조회하며, 사용자 닉네임을통해 나를 팔로워한 사용자를 찾을수 있습니다.[2글자이상]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공 - mutualFollow - ture 맞팔로워,  mutualFollow - false 팔로워 ", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value =  "{\"success\": true, \"message\": \"조회 성공\", \"data\": {\"hasNext\": false, \"follows\": [{\"followId\": 1, \"memberId\": 1, \"nickname\": \"닉네임\", \"profile\": \"http://www.example.com\", \"mutualFollow\": true}]}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/followers")
    public ResponseEntity<?> getFollowers(@RequestParam(value = "lastId",required = false) Long lastId,
                                          @RequestParam(value = "nickname",required = false) String nickname,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails,
                                          @Parameter(example = "{\"size\":10}") @PageableDefault Pageable pageable){

        Long followId = getMember(principalDetails);
        FollowResponse followers = followService.getFollowers(followId,nickname,lastId,pageable);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",followers));
    }

    @Operation(summary = "팔로잉 목록(무한 스크롤)", description = "팔로잉 목록을 조회하며, 사용자 닉네임을통해 내가 팔로잉한 사용자를 찾을수 있습니다.[2글자이상]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value =  "{\"success\": true, \"message\": \"조회 성공\", \"data\": {\"hasNext\": false, \"follows\": [{\"followId\": 1, \"memberId\": 1, \"nickname\": \"닉네임\", \"profile\": \"http://www.example.com\"}]}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/followings")
    public ResponseEntity<?> getFollows(@RequestParam(value = "lastId",required = false) Long lastId,
                                        @RequestParam(value = "nickname",required = false) String nickname,
                                        @AuthenticationPrincipal PrincipalDetails principalDetails,
                                        @Parameter(example = "{\"size\":10}") @PageableDefault Pageable pageable){
        Long followId = getMember(principalDetails);
        FollowResponse followings = followService.getFollowings(followId, nickname, lastId, pageable);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공", followings));
    }

    private static Long getMember(PrincipalDetails principalDetails) {
        Long followId = principalDetails.getMember().getId();
        return followId;
    }
}
