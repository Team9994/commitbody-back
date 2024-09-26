package team9499.commitbody.domain.block.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team9499.commitbody.domain.block.event.CancelBlockMemberEvent;
import team9499.commitbody.domain.block.event.ElsBlockMemberEvent;
import team9499.commitbody.domain.block.servcice.BlockMemberService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BlockMemberController {

    private final BlockMemberService blockMemberService;
    private final ApplicationEventPublisher eventPublisher;

    @Operation(summary = "사용자 차단", description = "하나의 API를 통해 차단/해제 요청을 수행합니다.",tags = "팔로워")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - 각 요청 응답에 맞게 [차단 해제, 차단 성공]을 반환 합니다.", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"차단 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 사용자 존재하지 않을시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/block/member")
    public ResponseEntity blockMember(@Parameter(schema = @Schema(example = "{\"blockedId\":1}")) @RequestBody Map<String,Long> reqeust,
                                      @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long blockedId = reqeust.get("blockedId");
        Long blockerId = principalDetails.getMember().getId();
        String status = blockMemberService.blockMember(blockerId, blockedId);
        eventPublisher.publishEvent(new ElsBlockMemberEvent(blockerId,blockedId,status));
        eventPublisher.publishEvent(new CancelBlockMemberEvent(blockedId,blockerId,status));
        return ResponseEntity.ok(new SuccessResponse<>(true,status));
    }
}
