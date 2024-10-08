package team9499.commitbody.global.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import team9499.commitbody.global.notification.domain.Notification;
import team9499.commitbody.global.notification.domain.NotificationType;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long>,CustomNotificationRepository {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = 1 WHERE n.receiver.id IN :receiverId AND n.isRead = 0")
    int updateRead(@Param("receiverId") Long receiverId);

    void deleteByReceiverIdAndSenderIdAndNotificationType(Long receiverId, Long senderId, NotificationType notificationType);

    boolean existsByReceiverIdAndIsRead(Long receiverId, Integer isRead);

    Notification findByCommentId(Long commentId);
}
