package team9499.commitbody.global.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.notification.dto.response.NotificationResponse;
import team9499.commitbody.global.notification.service.NotificationService;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

@Tag(name = "알림",description = "알림 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notification")
    public ResponseEntity<?> getAllNotification(@RequestParam(value = "lastId", required = false) Long lastId,
                                                @AuthenticationPrincipal PrincipalDetails principalDetails,
                                                Pageable pageable){
        Long memberId = getMemberId(principalDetails);
        NotificationResponse allNotification = notificationService.getAllNotification(memberId, lastId, pageable);

        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",allNotification));
    }

    @Operation(summary = "알림 일괄 읽기", description = "알림 버튼 클릭시 현재 알림을 모두 읽기 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/notification/read")
    public void updateNotificationRead(@AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        notificationService.updateRead(memberId);
    }

    @Operation(summary = "새알림 유뮤 체크", description = "새로운 알림이 존재하는지 파악합니다. 새로운 알림 존재시 true, 존재하지 않을시에는 false를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value =  "{\"success\": true, \"message\": \"새로운 알림 상태\", \"data\": true}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/notification/check")
    public ResponseEntity<?> checkNotification(@AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        boolean status = notificationService.newNotificationCheck(memberId);
        return ResponseEntity.ok(new SuccessResponse<>(true,"새로운 알림 상태",status));
    }

    private static Long getMemberId(PrincipalDetails principalDetails) {
        return principalDetails.getMember().getId();
    }
}
