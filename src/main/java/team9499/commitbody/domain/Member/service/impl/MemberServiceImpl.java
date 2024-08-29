package team9499.commitbody.domain.Member.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.response.MemberMyPageResponse;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.Member.service.MemberService;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.aws.s3.S3Service;

import java.time.LocalDate;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final FollowRepository followRepository;
    private final S3Service s3Service;


    /**
     * 마이페이지 조회 API
     * @param memberId  현재 로그인한 사용자 정보
     */
    @Override
    public MemberMyPageResponse getMyPage(Long memberId) {
        Member member = getMember(memberId);
        int countFollower = (int) followRepository.getCountFollower(memberId);
        int countFollowing = (int)followRepository.getCountFollowing(memberId);

        return new MemberMyPageResponse(member.getId(),member.getNickname(),member.getProfile(),countFollower,countFollowing);
    }


    /**
     *  프로필 업데이트
     * @param memberId  수정할 사용자 ID
     * @param nickname  닉네임
     * @param gender    성별
     * @param birthDay  생년월일
     * @param height    키
     * @param weight    몸무게
     * @param boneMineralDensity  골근격량
     * @param bodyFatPercentage  체지방량
     * @param deleteProfile 기본 프로필 변경 여부
     * @param file
     */
    @Override
    public void updateProfile(Long memberId, String nickname, Gender gender, LocalDate birthDay, Float height, Float weight, Float boneMineralDensity,
                              Float bodyFatPercentage, boolean deleteProfile,MultipartFile file) {
        Member member = getMember(memberId);
        String profile = s3Service.updateProfile(file, member.getProfile(),deleteProfile);
        member.updateProfile(nickname,gender,birthDay,height,weight,boneMineralDensity,bodyFatPercentage,profile);
    }


    private Member getMember(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
        return member;
    }
}
