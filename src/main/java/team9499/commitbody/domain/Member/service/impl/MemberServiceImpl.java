package team9499.commitbody.domain.Member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.response.MemberMyPageResponse;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.Member.service.MemberService;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final FollowRepository followRepository;

    /**
     * 마이페이지 조회 API
     * @param memberId  현재 로그인한 사용자 정보
     */
    @Override
    public MemberMyPageResponse getMyPage(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
        int countFollower = (int) followRepository.getCountFollower(memberId);
        int countFollowing = (int)followRepository.getCountFollowing(memberId);

        return new MemberMyPageResponse(member.getId(),member.getNickname(),member.getProfile(),countFollower,countFollowing);
    }
}
