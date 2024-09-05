package team9499.commitbody.domain.block.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team9499.commitbody.domain.block.event.ElsBlockMemberEvent;
import team9499.commitbody.domain.block.servcice.BlockMemberService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.SuccessResponse;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BlockMemberController {

    private final BlockMemberService blockMemberService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/block/member")
    public ResponseEntity blockMember(@Parameter(schema = @Schema(example = "{\"blockedId\":1}")) @RequestBody Map<String,Long> reqeust,
                                      @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long blockedId = reqeust.get("blockedId");
        Long blockerId = principalDetails.getMember().getId();
        String status = blockMemberService.blockMember(blockerId, blockedId);
        eventPublisher.publishEvent(new ElsBlockMemberEvent(blockerId,blockedId,status));
        return ResponseEntity.ok(new SuccessResponse<>(true,status));
    }
}
