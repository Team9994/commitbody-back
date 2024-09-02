package team9499.commitbody.global.notification.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import team9499.commitbody.global.notification.service.NotificationService;

@Component
@RequiredArgsConstructor
public class NotificationHandler {

    private final NotificationService notificationService;

    @EventListener
    public void Following(FollowingEvent followingEvent){
        notificationService.sendFollowing(followingEvent.getFollower(), followingEvent.getFollowing());
    }
}
