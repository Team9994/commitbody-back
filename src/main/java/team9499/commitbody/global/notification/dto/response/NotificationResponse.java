package team9499.commitbody.global.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.global.notification.dto.NotificationDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {

    private boolean hasNext;

    private List<NotificationDto> notifications;
}
