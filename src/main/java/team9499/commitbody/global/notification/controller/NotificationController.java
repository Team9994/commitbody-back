package team9499.commitbody.global.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.notification.service.NotificationService;
import team9499.commitbody.global.payload.SuccessResponse;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/notification/read")
    public void updateNotificationRead(@AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        notificationService.updateRead(memberId);
    }

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
