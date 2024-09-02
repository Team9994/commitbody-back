package team9499.commitbody.global.notification.event;

import lombok.Getter;

@Getter
public class DeleteFollowEvent {

    private Long followerId;

    private Long followingId;

    public DeleteFollowEvent(Long followerId, Long followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }
}
