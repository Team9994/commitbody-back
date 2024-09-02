package team9499.commitbody.global.notification.event;

import lombok.Getter;

@Getter
public class FollowingEvent {

    private Long follower;

    private Long following;

    public FollowingEvent(Long follower, Long following) {
        this.follower = follower;
        this.following = following;
    }
}
