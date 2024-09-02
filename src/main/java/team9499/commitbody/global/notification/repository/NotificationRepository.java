package team9499.commitbody.global.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.global.notification.domain.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {
}
