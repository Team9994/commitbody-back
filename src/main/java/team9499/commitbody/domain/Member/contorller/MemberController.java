package team9499.commitbody.domain.Member.contorller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.Member.dto.response.MemberInfoResponse;
import team9499.commitbody.domain.Member.service.MemberDocService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MemberController {

    private final MemberDocService memberDocService;

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
