package team9499.commitbody.global.authorization.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.authorization.domain.RefreshToken;
import team9499.commitbody.global.authorization.repository.RefreshTokenRepository;
import team9499.commitbody.global.authorization.service.AuthorizationService;
import team9499.commitbody.global.authorization.service.OpenIdConnectService;
import team9499.commitbody.global.utils.JwtUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthorizationServiceImpl implements AuthorizationService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;
    @Value("${jwt.refresh}")
    private int expired;        // 만료시간
    @Qualifier("Kakao")
    private final OpenIdConnectService kakaoOpenIdConnectService;
    @Qualifier("Google")
    private final OpenIdConnectService googleOpenIdConnectService;
    private final String REFRESH_TOKEN = "refreshToken";

    @Override
    public Map<String,String> authenticateOrRegisterUser(LoginType loginType,String socialJwt) {
        String socialId = "";
        LoginType socialType = null;

        if(loginType.equals(LoginType.KAKAO)){
            socialId = kakaoOpenIdConnectService.getSocialId(socialJwt);
            socialType = LoginType.KAKAO;
        }else {
            socialId = googleOpenIdConnectService.getSocialId(socialJwt);
            socialType = LoginType.GOOGLE;
        }

        Long memberId = null;
        Optional<Member> optionalMember = memberRepository.findBySocialId(socialId);
        if (!optionalMember.isPresent()){
            Member member = memberRepository.save(Member.createSocialId(socialId,socialType));
            optionalMember = Optional.of(member);
            memberId = member.getId();
        }
        else {      //로그인인 경우
            Member member = optionalMember.get();
            memberId = member.getId();
            setSecurityMemberInfo(member);
        }

        Map<String, String> tokenMap = jwtUtils.generateAuthTokens(MemberDto.builder().memberId(memberId).build());
        SaveRefreshToken(memberId, optionalMember, tokenMap.get(REFRESH_TOKEN));
        return tokenMap;
    }

    /*
    로그인한 사용자의 정보를 시큐리티 컨텍스트의 저장
     */
    private static void setSecurityMemberInfo(Member member) {
        PrincipalDetails principalDetails = new PrincipalDetails(member);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }


    /*
    레디스의 정보가 서버의 문제로 인해 삭제될수있기때문에 MySQL에 리프레쉬 토큰을 저장
     */
    private void SaveRefreshToken(Long memberId, Optional<Member> optionalMember, String refreshToken) {
        boolean existsByMemberId = refreshTokenRepository.existsByMemberId(memberId);
        if(!existsByMemberId) {
            refreshTokenRepository.save(
                    RefreshToken.of(optionalMember.get(), refreshToken, LocalDateTime.now().plusMonths(expired))
            );
        }
    }
}
