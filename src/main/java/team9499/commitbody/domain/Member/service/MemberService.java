package team9499.commitbody.domain.Member.service;

import team9499.commitbody.domain.Member.dto.response.MemberMyPageResponse;

public interface MemberService{

    MemberMyPageResponse getMyPage(Long memberId);
}
