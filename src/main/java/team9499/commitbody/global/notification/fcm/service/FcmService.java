package team9499.commitbody.global.notification.fcm.service;

import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.global.redis.RedisService;

import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class FcmService {

    private final RedisService redisService;

    public void sendFollowMessage(String memberId){
        String fcmToken = redisService.getFCMToken(memberId);
        if (fcmToken.equals("")) return;
        log.info("fcm={}",fcmToken);

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle("팔로우 요청")
                        .setBody("떙떙 사용자가 팔로우를 했습니다.")
                        .build())
                .putData("click_action", "http://localhost:8080")
                .build();
        send(message);
    }

    public void send(Message message) {
        ApiFuture<String> response = FirebaseMessaging.getInstance().sendAsync(message);

        // 전송 결과 확인
        try {
            String messageId = response.get(); // 성공 시 메시지 ID 반환
            log.info("응값값 ={}",response);
            System.out.println("알림이 성공적으로 전송되었습니다. 메시지 ID: " + messageId);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("알림 전송에 실패했습니다: " + e.getMessage());
        }
    }
}
