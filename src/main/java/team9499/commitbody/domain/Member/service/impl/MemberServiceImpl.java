package team9499.commitbody.domain.Member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.response.MemberMyPageResponse;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.Member.service.MemberDocService;
import team9499.commitbody.domain.Member.service.MemberService;
import team9499.commitbody.domain.article.service.ElsArticleService;
import team9499.commitbody.domain.block.servcice.BlockMemberService;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.aws.s3.S3Service;
import team9499.commitbody.global.redis.RedisService;

import java.time.LocalDate;

@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final static String NOTIFICATION = "알림 수신";
    private final static String NOT_NOTIFICATION = "알림 미수신";
    private final static String MY_PAGE = "myPage";
    private final static String THEIR_PAGE = "theirPage";

    private final MemberRepository memberRepository;
    private final FollowRepository followRepository;
    private final BlockMemberService blockMemberService;
    private final MemberDocService memberDocService;
    private final ElsArticleService elsArticleService;
    private final RedisService redisService;
    private final S3Service s3Service;


    @Override
    public MemberMyPageResponse getMyPage(Long memberId, String nickname) {
        Member member = filterWithDrawnMember(nickname);
        return handleMyPageResponse(memberId,member);
    }

    @Override
    public void updateProfile(Long memberId, String nickname, Gender gender, LocalDate birthDay, Float height, Float weight,
                              Float boneMineralDensity, Float bodyFatPercentage, boolean deleteProfile, MultipartFile file) {
        Member member = getMember(memberId);
        String beforeNickname = member.getNickname();   // 변경하기전 닉네임
        String profile = s3Service.updateProfile(file, member.getProfile(), deleteProfile);
        member.updateProfile(nickname, gender, birthDay, height, weight, boneMineralDensity, bodyFatPercentage, profile);
        handleUpdateProfile(memberId, nickname, member, profile, beforeNickname);
    }

    @Override
    public boolean getNotification(Long memberId) {
        Member member = redisService.getMemberDto(String.valueOf(memberId)).get();
        return member.isNotificationEnabled();
    }

    /**
     * 사용자가 알림 수신 여부를 선택합니다.
     *
     * @param memberId 현재 로그인한 사용자 ID
     * @return 알림 수신 , 알림 미수신
     */
    @Override
    public String updateNotification(Long memberId) {
        Member member = getMember(memberId);
        boolean notificationEnabled = member.isNotificationEnabled();
        member.updateNotification(!notificationEnabled);        // 알림 수신 유무 반대로 저장
        redisService.updateMember(String.valueOf(member), member);       // 레디시의 정보 업데이트
        return notificationEnabled ? NOT_NOTIFICATION : NOTIFICATION;
    }

    private Member filterWithDrawnMember(String nickname) {
        return memberRepository.findByNickname(nickname).filter(m -> !m.isWithdrawn())
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
    }

    private MemberMyPageResponse handleMyPageResponse(Long memberId, Member member) {
        boolean blockStatus = blockMemberService.checkBlock(member.getId(), memberId);
        int countFollower = (int) followRepository.getCountFollower(member.getId());
        int countFollowing = (int) followRepository.getCountFollowing(member.getId());
        return getMemberMyPageResponse(memberId, member, blockStatus, countFollower, countFollowing);
    }

    private MemberMyPageResponse getMemberMyPageResponse(Long memberId, Member member, boolean blockStatus,
                                                         int countFollower, int countFollowing) {
        if (member.getId().equals(memberId)){
            return MemberMyPageResponse.myPageOf(member, MY_PAGE, blockStatus, countFollower, countFollowing);
        }
        FollowType followStatus = followRepository.followStatus(memberId, member.getId());
        return MemberMyPageResponse.otherPageOf(member, THEIR_PAGE, blockStatus, countFollower, countFollowing, followStatus);
    }

    private void handleUpdateProfile(Long memberId, String nickname, Member member, String profile, String beforeNickname) {
        redisService.updateMember(String.valueOf(memberId), member); // 레디스 회원 정보 업데이트
        memberDocService.updateMemberDocAsync(String.valueOf(memberId), nickname, profile);   // 사용자 인덱스 업데이트
        elsArticleService.updateWriterAsync(beforeNickname, nickname);   // 게시글의 작성된 사용자 닉네임 업데이트
    }

}
