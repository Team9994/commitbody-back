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

    /**
     * 팔로우 관련 알림 전송 메서드
     * @param followerId    발신자
     * @param followingId   수신자
     */
    @Async
    @Override
    public void sendFollowing(Long followerId, Long followingId) {
        Member followerMember = redisService.getMemberDto(String.valueOf(followerId)).get();        // 발신자
        Member followingMember = redisService.getMemberDto(String.valueOf(followingId)).get();      // 수신자
        String content = followerMember.getNickname()+"님이 회원님을 팔로우하기 시작했어요.";

        Notification notification = Notification.of(content, NotificationType.FOLLOW, followingMember, followerMember);
        asyncSave(notification);        // 비동기 저장

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
            Notification notification = Notification.of(content, NotificationType.REPLY_COMMENT, replyMember, member);
            asyncSave(notification);
        }
    }

    @Override
    public void sendComment(Member member,Long receiverId,String articleTitle, String commentContent,String commentId) {
        Member receiverMember = redisService.getMemberDto(String.valueOf(receiverId)).get();// 댓글 알림 수신자.
        String content = member.getNickname()+"님이 회원님의 게시글에 댓글을 남겼어요: "+commentContent;

        if (receiverMember.isNotificationEnabled() && member.getId() != receiverMember.getId()) {
            fcmService.sendComment(String.valueOf(receiverMember.getId()),articleTitle,content,commentId);
            Notification notification = Notification.of(content, NotificationType.COMMENT,receiverMember, member);
            asyncSave(notification);
        }
    }

    private Member getReplyMember(String replyNickname) {
        Member replyMember = memberRepository.findByNickname(replyNickname).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
        return replyMember;
    }

}
