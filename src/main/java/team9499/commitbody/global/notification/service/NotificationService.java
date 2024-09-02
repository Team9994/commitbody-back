package team9499.commitbody.global.notification.service;

public interface NotificationService {

    void sendFollowing(Long followerId, Long followingId);
    void updateRead(Long receiverId);
    void deleteNotification(Long followerId, Long followingId);
    boolean newNotificationCheck(Long memberId);
}
