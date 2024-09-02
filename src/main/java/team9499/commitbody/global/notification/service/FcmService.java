package team9499.commitbody.global.notification.service;

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

    public void sendFollowingMessage(String followingId,String content){
        String fcmToken = redisService.getFCMToken(followingId);

        if (fcmToken.equals("")) return;

        // 메시 객체 생성
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle("팔로우 알림")
                        .setBody(content)
                        .build())
                // TODO: 2024-09-02 프론트앤드 url 주소 확정시 url 추가
//                .putData("click_action", "http://localhost:8080")
                .build();

        send(message);
    }

    private void send(Message message) {
        ApiFuture<String> response = FirebaseMessaging.getInstance().sendAsync(message);    // 비동기 전송
        // 전송 결과 확인
        try {
            String messageId = response.get(); // 성공 시 메시지 ID 반환
            log.info("알림이 성공적으로 전송되었습니다. 메시지 ID: {}", messageId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("알림 전송에 실패했습니다: " + e.getMessage());
        }
    }


}
