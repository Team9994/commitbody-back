package team9499.commitbody.global.notification.service;

import org.springframework.data.domain.Pageable;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.global.notification.dto.response.NotificationResponse;

public interface NotificationService {

    NotificationResponse getAllNotification(Long memberId, Long lastId, Pageable pageable);

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
