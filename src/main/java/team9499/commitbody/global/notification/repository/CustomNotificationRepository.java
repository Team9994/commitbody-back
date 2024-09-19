package team9499.commitbody.global.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.global.notification.dto.NotificationDto;

public interface CustomNotificationRepository {

    Slice<NotificationDto> getAllNotification(Long memberId, Long lastId, Pageable pageable);
}
