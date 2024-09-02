package team9499.commitbody.global.notification.service;

public interface NotificationService {

    void sendFollowing(Long followerId, Long followingId);

    void updateRead(Long receiverId);
}
