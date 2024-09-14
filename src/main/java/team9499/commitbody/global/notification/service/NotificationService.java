package team9499.commitbody.global.notification.service;

import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;

public interface NotificationService {

    void sendFollowing(Long followerId, Long followingId);

    void updateRead(Long receiverId);

    void deleteNotification(Long followerId, Long followingId);

    boolean newNotificationCheck(Long memberId);

    void sendReplyComment(Member member, String replyNickname,String articleTitle ,String commentContent,String commentId,Long articleId);

    void sendComment(Member member,Long receiverId,String articleTitle ,String commentContent,String commentId,Long articleId);

    void sendArticleLike(Member member, Long receiverId,Long articleId, boolean status);

    void sendCommentLike(Member member, Long receiverId, Long commentId,Long articleId, boolean status);

    void updateNotification(Long commentId, String content);
}
