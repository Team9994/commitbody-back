package team9499.commitbody.global.notification.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.global.utils.BaseTime;

import static ch.qos.logback.classic.spi.ThrowableProxyVO.build;

@Data
@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@ToString(exclude = {"receiver","sender"})
public class Notification extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;            // id(pk)

    @Column(length = 1000)
    private String content;  // 알림 내용

    private Integer isRead;       // 알림 읽음 상태   (0 : 안읽음 상태 , 1 : 읽은 상태)

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;  // 알림 종류(팔로우, 댓글 , 좋아요, 전체알림)

    @JoinColumn(name = "receiver_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Member receiver;      // 수신자

    @JoinColumn(name = "sender_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Member sender;      // 발신자

    private Long commentId;     // 댓글 ID

    public static Notification of(String content,NotificationType notificationType,Member receiver,Member sender,Long commentId){
        NotificationBuilder notificationBuilder = Notification.builder().content(content).notificationType(notificationType).receiver(receiver).sender(sender).isRead(0);
        if (content!=null)  notificationBuilder.commentId(commentId); // 댓글의 대한 알림일 경우
        
        return notificationBuilder.build();
    }

    public void updateContent(String content){
        this.content = content;
    }
}
