package team9499.commitbody.testel;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.notification.fcm.service.FcmService;
import team9499.commitbody.global.redis.RedisService;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final FcmService fcmService;
    private final RedisService redisService;


    @PostMapping("/api/v1/save")
    public ResponseEntity<?> save(@RequestParam("token")String token,
            @AuthenticationPrincipal PrincipalDetails principalDetails){
        redisService.setFCM(String.valueOf(principalDetails.getMember().getId()),token);
        return ResponseEntity.ok("알림전송 성공");
    }


    @PostMapping("/api/v1/send")
    public ResponseEntity<?> test(@AuthenticationPrincipal PrincipalDetails principalDetails){
        fcmService.sendFollowMessage(String.valueOf(principalDetails.getMember().getId()));
        return ResponseEntity.ok("알림전송 성공");
    }
}
