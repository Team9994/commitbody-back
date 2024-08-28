package team9499.commitbody.domain.Member.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.Member.dto.response.MemberInfoResponse;
import team9499.commitbody.domain.Member.service.MemberDocService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MemberController {

    private final MemberDocService memberDocService;

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
        Long memberId = principalDetails.getMember().getId();
        int fromValue = (from != null) ? from : 0; // 기본값 0
        int sizeValue = (size != null) ? size : 10; // 기본값 10

        MemberInfoResponse memberForNickname = memberDocService.findMemberForNickname(memberId, nickname, fromValue, sizeValue);

        return ResponseEntity.ok(new SuccessResponse<>(true,"검색 성공",memberForNickname));
    }
}
