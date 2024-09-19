package team9499.commitbody.global.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import team9499.commitbody.global.notification.domain.Notification;
import team9499.commitbody.global.utils.TimeConverter;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDto {

    private Long id;

    private String content;

    private String time;

    private String profile;

    private String nickname;

    private Long articleId;

    public static NotificationDto of (Notification notification){
        NotificationDtoBuilder builder = NotificationDto.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .time(TimeConverter.converter(notification.getCreatedAt()))
                .profile(notification.getReceiver().getProfile()).nickname(notification.getSender().getNickname());

        if (notification.getArticleId()!=null){
            builder.articleId(notification.getArticleId());
        }
        return builder.build();
    }

}
