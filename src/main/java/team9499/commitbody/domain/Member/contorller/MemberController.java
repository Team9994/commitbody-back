package team9499.commitbody.domain.Member.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.dto.request.ProfileUpdateRequest;
import team9499.commitbody.domain.Member.dto.response.MemberInfoResponse;
import team9499.commitbody.domain.Member.dto.response.MemberMyPageResponse;
import team9499.commitbody.domain.Member.service.MemberDocService;
import team9499.commitbody.domain.Member.service.MemberService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MemberController {

    private final MemberDocService memberDocService;
    private final MemberService memberService;

    @Operation(summary = "사용자 검색", description = "사용자 닉네임을 통해 회원을 검색합니다. default size : 10, default form : 0",tags = "팔로워")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value =  "{\"success\": true, \"message\": \"검색 성공\", \"data\": {\"totalCount\": 2, \"members\": [{\"memberId\": 1, \"nickname\": \"https://example.com\", \"profile\": \"첫번째 닉네임\"}, {\"memberId\": 2, \"nickname\": \"https://example.com\", \"profile\": \"두번째 닉네임\"}]}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/search/member")
    public ResponseEntity<?> getTest(@RequestParam(value = "nickname",required = false) String nickname,
                                     @RequestParam(value = "size",required = false) Integer size,
                                     @RequestParam(value = "from",required = false) Integer from,
                                     @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        int fromValue = (from != null) ? from : 0; // 기본값 0
        int sizeValue = (size != null) ? size : 10; // 기본값 10

        MemberInfoResponse memberForNickname = memberDocService.findMemberForNickname(memberId, nickname, fromValue, sizeValue);

        return ResponseEntity.ok(new SuccessResponse<>(true,"검색 성공",memberForNickname));
    }

    @Tag(name = "프로필",description = "프로필 관련 API")
    @Operation(summary = "마이페이지 - 사용자 정보 조회", description = "사용자의 정보(닉네임,프로필사진,팔로워/팔로잉)을 조회합니다, pageType[myPage: 마이페이지, theirPage: 상대 페이지], followStatus[FOLLOW_ONLY : 상대방만 팔로워, FOLLOW : 상대방을 팔로우, NEITHER : 서로 팔로워 하지않음]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200_1", description = "마이페이지 조회시", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value =  "{\"success\":true,\"message\":\"조회 성공\",\"data\":{\"memberDto\":{\"memberId\":37,\"nickname\":\"안녕하세\",\"profile\":\"https://doyg075k8m500.cloudfront.net/profile_type_4.png\",\"height\":\"199.0\",\"weight\":\"29.0\",\"birthday\":\"1999-09-28\",\"gender\":\"MALE\"},\"pageType\":\"myPage\",\"followerCount\":1,\"followingCount\":1,\"blockStatus\":false}}"))),
            @ApiResponse(responseCode = "200_2", description = "상대페이지 조회시 : blockStatus : 상대방 차단 여부, accountStatus : [PRIVATE,PUBLIC] 상대방의 계정 공개 여부", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value =  "{\"success\":true,\"message\":\"조회 성공\",\"data\":{\"memberDto\":{\"memberId\":35,\"nickname\":\"라123\",\"profile\":\"https://doyg075k8m500.cloudfront.net/profile_type_2.png\",\"height\":\"12.0\",\"weight\":\"12.0\",\"birthday\":\"1222-12-12\",\"gender\":\"MALE\",\"boneMineralDensity\":12.0,\"bodyFatPercentage\":12.0},\"pageType\":\"theirPage\",\"followerCount\":0,\"followingCount\":1,\"followStatus\":\"FOLLOW_ONLY\",\"blockStatus\":false}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 사용자 존재하지 않을시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/my-page/{nickname}")
    public ResponseEntity<?> myPage(@PathVariable("nickname") String nickname,
                                    @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        MemberMyPageResponse myPage = memberService.getMyPage(memberId,nickname);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",myPage));
    }


    @Operation(summary = "프로필 수정", description = "해당 사용자의 대한 프로필 정보를 수정가능합니다. 기본 프로필로 설정시 deleteProfile =true, 아닐시는 false를 사용합니다.",tags = "프로필")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"업데이트 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 파일 용량 초과(5MB 이하만 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"저장 가능한 용량을 초과 했습니다.\"}"))),
            @ApiResponse(responseCode = "400_4", description = "BADREQUEST - 불가능한 파일 형식 저장시(jpeg, jpg, png, gif 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"올바른 파일 형식이 아닙니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateProfile(@Valid @RequestPart("profileUpdateRequest") ProfileUpdateRequest profileUpdateRequest, BindingResult result,
                                           @RequestPart(name = "file",required = false) MultipartFile file,
                                           @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        memberService.updateProfile(memberId, profileUpdateRequest.getNickname(),profileUpdateRequest.getGender(),profileUpdateRequest.getBirthDay(), profileUpdateRequest.getHeight(),
                profileUpdateRequest.getWeight(),profileUpdateRequest.getBoneMineralDensity(), profileUpdateRequest.getBodyFatPercentage(),profileUpdateRequest.isDeleteProfile(),file);
        return ResponseEntity.ok(new SuccessResponse<>(true,"업데이트 성공"));
    }

    @Operation(summary = "알림 수신 여부 - 조회", description = "사용자의 알림 수신 여부를 조회합니다. [true : 알림 수신, false : 알림 미수신]",tags = "프로필")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"알림 수신 유뮤\",\"data\":true}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 사용자 존재하지 않을시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @GetMapping("/notification/settings")
    public ResponseEntity<?> getNotificationSetting(@AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        boolean notification = memberService.getNotification(memberId);
        return ResponseEntity.ok(new SuccessResponse<>(true,"알림 수신 유뮤",notification));
    }


    @Operation(summary = "알림 수신 여부 - 설정", description = "알림 설정을 업데이트 합니다. [수신시 : 알림 수신 , 미수신시 : 알림 미수신]",tags = "프로필")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"알림 수신\""))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 사용자 존재하지 않을시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping("/notification/settings")
    public ResponseEntity<?> setNotificationSetting(@AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);

        String updateNotification = memberService.updateNotification(memberId);
        return ResponseEntity.ok(new SuccessResponse<>(true,updateNotification));
    }

    private static Long getMemberId(PrincipalDetails principalDetails) {
        return principalDetails.getMember().getId();
    }
}
