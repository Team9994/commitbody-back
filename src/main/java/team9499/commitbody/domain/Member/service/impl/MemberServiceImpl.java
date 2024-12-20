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

    /**
     * 마이페이지 조회 API
     *
     * @param memberId 현재 로그인한 사용자 정보
     */
    @Override
    public MemberMyPageResponse getMyPage(Long memberId, String nickname) {
        Member member = filterWithDrawnMember(nickname);
        MemberMyPageResponse.MemberMyPageResponseBuilder myPageBuilder = getMemberMyPageResponseBuilder(memberId, member);
        return handleMyPageResponse(memberId, member, myPageBuilder);
    }

    /**
     * 프로필 업데이트
     *
     * @param memberId           수정할 사용자 ID
     * @param nickname           닉네임
     * @param gender             성별
     * @param birthDay           생년월일
     * @param height             키
     * @param weight             몸무게
     * @param boneMineralDensity 골근격량
     * @param bodyFatPercentage  체지방량
     * @param deleteProfile      기본 프로필 변경 여부
     */
    @Override
    public void updateProfile(Long memberId, String nickname, Gender gender, LocalDate birthDay, Float height, Float weight,
                              Float boneMineralDensity, Float bodyFatPercentage, boolean deleteProfile, MultipartFile file) {
        Member member = getMember(memberId);
        String beforeNickname = member.getNickname();   // 변경하기전 닉네임
        String profile = s3Service.updateProfile(file, member.getProfile(), deleteProfile);
        member.updateProfile(nickname, gender, birthDay, height, weight, boneMineralDensity, bodyFatPercentage, profile);
        handleUpdateProfile(memberId, nickname, member, profile, beforeNickname);
    }


    /**
     * 현자 사용자의 알림 유뮤를 조회합니다.
     *
     * @param memberId 로그인한 사용자 아이디
     * @return true 알림 수신 false 알림 미수신
     */
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

    private MemberMyPageResponse.MemberMyPageResponseBuilder getMemberMyPageResponseBuilder(Long memberId, Member member) {
        boolean blockStatus = blockMemberService.checkBlock(member.getId(), memberId);

        int countFollower = (int) followRepository.getCountFollower(member.getId());
        int countFollowing = (int) followRepository.getCountFollowing(member.getId());
        return MemberMyPageResponse.builder().nickname(member.getNickname()).profile(member.getProfile())
                .followerCount(countFollower).followingCount((countFollowing)).blockStatus(blockStatus);
    }

    private MemberMyPageResponse handleMyPageResponse(Long memberId, Member member,
                                                      MemberMyPageResponse.MemberMyPageResponseBuilder myPageBuilder) {
        if (member.getId().equals(memberId)) {
            return myPageBuilder.memberId(memberId).pageType(MY_PAGE).build();
        }
        FollowType followStatus = followRepository.followStatus(memberId, member.getId());  // 상대방과 팔로우 관계를 검사
        return myPageBuilder.followStatus(followStatus).accountStatus(member.getAccountStatus())
                .memberId(member.getId()).pageType(THEIR_PAGE).build();
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
    }

    private void handleUpdateProfile(Long memberId, String nickname, Member member, String profile, String beforeNickname) {
        redisService.updateMember(String.valueOf(memberId), member); // 레디스 회원 정보 업데이트
        memberDocService.updateMemberDocAsync(String.valueOf(memberId), nickname, profile);   // 사용자 인덱스 업데이트
        elsArticleService.updateWriterAsync(beforeNickname, nickname);   // 게시글의 작성된 사용자 닉네임 업데이트
    }

}
