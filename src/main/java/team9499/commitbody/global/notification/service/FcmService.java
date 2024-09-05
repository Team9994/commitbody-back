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

    /**
     * 댓글의 답글이 달릴시 댓글 댓글 사용자에게 알림이 전송
     * @param receiverId 답글 알림 대상자
     * @param articleTitle  게시글 제목
     * @param content   알림 내용
     * @param commentId 작성된 댓글 ID
     */
    public void sendReplyComment(String receiverId,String articleTitle,String content,String commentId){
        String fcmToken = redisService.getFCMToken(receiverId);      // 수신자
        if (fcmToken.equals("")) return;

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(articleTitle+" 게시글 답글 알림")
                        .setBody(content)
                        .build())
                .putData("commentId",commentId)
                .build();
        send(message);
    }

    /**
     * 게시글의 타사용자의 댓글이 달리면 알림이 전송
     * @param receiverId    알림 수신자
     * @param articleTitle  게시글 제목
     * @param content   알림 내용
     * @param commentId 작성된 댓글 ID
     */
    public void sendComment(String receiverId,String articleTitle,String content,String commentId){
        String fcmToken = redisService.getFCMToken(receiverId);      // 수신자
        if (fcmToken.equals("")) return;

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(articleTitle+" 게시글 댓글 알림")
                        .setBody(content)
                        .build())
                .putData("commentId",commentId)
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
