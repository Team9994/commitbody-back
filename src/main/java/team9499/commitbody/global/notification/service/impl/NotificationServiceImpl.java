package team9499.commitbody.global.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.notification.domain.Notification;
import team9499.commitbody.global.notification.domain.NotificationType;
import team9499.commitbody.global.notification.repository.NotificationRepository;
import team9499.commitbody.global.notification.service.FcmService;
import team9499.commitbody.global.notification.service.NotificationService;
import team9499.commitbody.global.redis.RedisService;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final FcmService fcmService;
    private final NotificationRepository notificationRepository;
    private final RedisService redisService;
    private final MemberRepository memberRepository;

    /**
     * 비동기를 통한 알림 데이터 저장
     */
    @Async
    public void asyncSave(Notification notification){
        notificationRepository.save(notification);
    }

    @Async
    public void asyncDelete(Long receiverId, Long senderId, NotificationType notificationType){
        notificationRepository.deleteByReceiverIdAndSenderIdAndNotificationType(receiverId,senderId,notificationType);
    }

    /**
     * 팔로우 관련 알림 전송 메서드
     * @param followerId    발신자
     * @param followingId   수신자
     */
    @Async
    @Override
    public void sendFollowing(Long followerId, Long followingId) {
        Member followerMember = getReceiverMember(followerId);
        Member followingMember = getReceiverMember(followingId);
        String content = followerMember.getNickname()+"님이 회원님을 팔로우하기 시작했어요.";

        saveNotification(content, NotificationType.FOLLOW, followingMember, followerMember,null);

        if (followingMember.isNotificationEnabled()) // 알림성정울 true로 한 상태 일때 알림 전송
            fcmService.sendFollowingMessage(String.valueOf(followingMember.getId()),content);
    }

    @Override
    public void updateRead(Long receiverId) {
        notificationRepository.updateRead(receiverId);
    }

    /**
     * 팔로우 취소및 언팔 시에 알림 내역 삭제 메서드
     * @param followerId    발신자
     * @param followingId   수신자
     */
    @Override
    public void deleteNotification(Long followerId, Long followingId) {
        notificationRepository.deleteByReceiverIdAndSenderIdAndNotificationType(followingId,followerId,NotificationType.FOLLOW);
    }

    /**
     * 새로운 알림이 존재하는지 체크하는 메서드
     * @param memberId  사용자 ID
     * @return 새로운 알림이 존재서 TURE, 존재하지 않을시 FALSE
     */
    @Override
    public boolean newNotificationCheck(Long memberId) {
        return notificationRepository.existsByReceiverIdAndIsRead(memberId, 0);
    }

    @Async
    @Override
    public void sendReplyComment(Member member, String replyNickname,String articleTitle,String commentContent,String commentId) {
        Member replyMember = getReplyMember(replyNickname);     // 댓글 알림 수신자
        String content = member.getNickname()+"님이 회원님의 댓글에 답급을 남겼어요:"+commentContent;
        // 알림 기능을 사용하며, 만약 자신에게 담긴 답글의 경우 알림 이전송되지 않도록
        if (replyMember.isNotificationEnabled() && member.getId() != replyMember.getId()) {
            fcmService.sendReplyComment(String.valueOf(replyMember.getId()), articleTitle, content,commentId);
            saveNotification(content, NotificationType.REPLY_COMMENT, replyMember, member, Long.valueOf(commentId));
        }
    }

    @Async
    @Override
    public void sendComment(Member member,Long receiverId,String articleTitle, String commentContent,String commentId) {
        Member receiverMember = getReceiverMember(receiverId);
        String content = member.getNickname()+"님이 회원님의 게시글에 댓글을 남겼어요: "+commentContent;

        if (receiverMember.isNotificationEnabled() && !member.getId().equals(receiverMember.getId())) {
            fcmService.sendComment(String.valueOf(receiverMember.getId()),articleTitle,content,commentId);
            saveNotification(content, NotificationType.COMMENT, receiverMember, member, Long.valueOf(commentId));
        }
    }


    /**
     * 게시글 좋아요관련 알림 기능
     * 좋아요 성공시 알림전송시 및 데이터 저장, 좋아요 해제시 저장된 데이터 삭제
     * @param member    발신자
     * @param receiverId   수신자 ID
     * @param articleId 게시글 ID
     * @param status true : 좋아요 성공 , false : 좋아요 해제
     */
    @Async
    @Override
    public void sendArticleLike(Member member, Long receiverId,Long articleId,boolean status) {
        Member receiverMember = getReceiverMember(receiverId);// 댓글 알림 수신자
        String content = member.getNickname()+"님이 회원님 게시글에 좋아요를 눌렀어요";

        // 알림을 좋아요한 상태이며, 알림 수신여부 , 발신자와 수신자의 아이디가 같지 않을때만 알림 전송
        if (status && receiverMember.isNotificationEnabled() && !member.getId().equals(receiverMember.getId())){
            fcmService.sendArticleLike(String.valueOf(receiverId),String.valueOf(articleId),content);
            saveNotification(content, NotificationType.ARTICLE_LIKE, receiverMember, member,null);
        }else {     // 좋아요 해제 요청시에는 알림 데이터 삭제
            asyncDelete(receiverId, member.getId(), NotificationType.ARTICLE_LIKE);
        }

    }
    @Override
    public void sendCommentLike(Member member, Long receiverId,Long commentId,boolean status) {
        Member receiverMember = getReceiverMember(receiverId);
        String content = member.getNickname()+"님이 회원님 댓글에 좋아요를 눌렀어요";

        if (status && receiverMember.isNotificationEnabled() && !member.getId().equals(receiverMember.getId())){
            fcmService.sendArticleCommentLike(String.valueOf(receiverId),String.valueOf(commentId),content);
            saveNotification(content, NotificationType.COMMENT_LIKE, receiverMember, member,commentId);
        }else {     // 좋아요 해제 요청시에는 알림 데이터 삭제
            asyncDelete(receiverId, member.getId(), NotificationType.COMMENT_LIKE);
        }
    }

//
//    @Async
//    @Override
//    public void updateNotification(Long receiverId, Long senderId, String content, NotificationType notificationType) {
//        if (!receiverId.equals(senderId)) {
//            Notification notification = notificationRepository.findByReceiverIdAndSenderIdAndNotificationType(receiverId, senderId, notificationType).orElse(null);
//            if (notification != null) notification.updateContent(content);
//        }
//    }

    private Member getReplyMember(String replyNickname) {
        return memberRepository.findByNickname(replyNickname).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
    }

    private Member getReceiverMember(Long receiverId) {
        return redisService.getMemberDto(String.valueOf(receiverId)).get();// 댓글 알림 수신자.
    }

    /*
    알림의 데이터를 비동기 저장
     */
    private void saveNotification(String content, NotificationType replyComment, Member replyMember, Member member,Long commentId) {
        Notification notification = Notification.of(content, replyComment, replyMember, member,commentId);
        asyncSave(notification);
    }

}
