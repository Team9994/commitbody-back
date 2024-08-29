package team9499.commitbody.domain.Member.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
    @Operation(summary = "마이페이지 - 사용자 정보 조회", description = "사용자의 정보(닉네임,프로필사진,팔로워/팔로잉)을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value =  "{\"success\": true, \"message\": \"조회 성공\", \"data\": {\"memberId\": 1, \"nickname\": \"첫번쨰닉네임\", \"profile\": \"https://example.com\", \"followerCount\": 0, \"followingCount\": 1}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 사용자 존재하지 않을시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/myPage")
    public ResponseEntity<?> myPage(@AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        MemberMyPageResponse myPage = memberService.getMyPage(memberId);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",myPage));
    }

    private static Long getMemberId(PrincipalDetails principalDetails) {
        return principalDetails.getMember().getId();
    }
}
