package team9499.commitbody.global.notification.service;

import team9499.commitbody.domain.Member.domain.Member;

public interface NotificationService {

    void sendFollowing(Long followerId, Long followingId);
    void updateRead(Long receiverId);
    void deleteNotification(Long followerId, Long followingId);
    boolean newNotificationCheck(Long memberId);
    void sendReplyComment(Member member, String replyNickname,String articleTitle ,String commentContent,String commentId);
    void sendComment(Member member,Long receiverId,String articleTitle ,String commentContent,String commentId);
}
