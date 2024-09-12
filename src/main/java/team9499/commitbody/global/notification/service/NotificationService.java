package team9499.commitbody.global.notification.service;

import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.global.notification.domain.NotificationType;

public interface NotificationService {

    void sendFollowing(Long followerId, Long followingId);

    void updateRead(Long receiverId);

    void deleteNotification(Long followerId, Long followingId);

    boolean newNotificationCheck(Long memberId);

    void sendReplyComment(Member member, String replyNickname,String articleTitle ,String commentContent,String commentId);

    void sendComment(Member member,Long receiverId,String articleTitle ,String commentContent,String commentId);

    void sendArticleLike(Member member, Long receiverId,Long articleId, boolean status);

    void sendCommentLike(Member member,Long receiverId,Long commentId,boolean status);
}
