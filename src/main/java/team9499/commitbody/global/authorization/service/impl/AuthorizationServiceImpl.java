package team9499.commitbody.global.authorization.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.authorization.domain.RefreshToken;
import team9499.commitbody.global.authorization.dto.TokenInfoDto;
import team9499.commitbody.global.authorization.dto.TokenUserInfoResponse;
import team9499.commitbody.global.authorization.repository.RefreshTokenRepository;
import team9499.commitbody.global.authorization.service.AuthorizationService;
import team9499.commitbody.global.authorization.service.OpenIdConnectService;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.global.utils.JwtUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static team9499.commitbody.global.Exception.ExceptionStatus.*;
import static team9499.commitbody.global.Exception.ExceptionType.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthorizationServiceImpl implements AuthorizationService {

    private final RedisService redisService;
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
    private final String JOIN = "회원가입";
    private final String LOGIN = "로그인";
    private final String NICKNAME ="nickname_";

    @Override
    public Map<String,Object> authenticateOrRegisterUser(LoginType loginType,String socialJwt) {
        String socialId = "";
        LoginType socialType = null;
        String joinOrLogin = "";

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
            joinOrLogin = JOIN;
        }
        else {      //로그인인 경우
            Member member = optionalMember.get();
            memberId = member.getId();
            setSecurityMemberInfo(member);
            joinOrLogin = LOGIN;
        }

        Map<String, Object> tokenMap = new LinkedHashMap<>(jwtUtils.generateAuthTokens(MemberDto.builder().memberId(memberId).build()));
        tokenMap.put("authMode",joinOrLogin);       // 로그인 / 회원가입을 구분하기위함

        if (joinOrLogin.equals(LOGIN)) tokenMap.put("tokenInfo",TokenInfoDto.of(memberId));     //로그인일 경우에만 JWT토큰의대한 정보를 담는다
        SaveRefreshToken(memberId, optionalMember, (String) tokenMap.get(REFRESH_TOKEN));
        return tokenMap;
    }

    @Override
    public TokenUserInfoResponse additionalInfoSave(String nickName, Gender gender, LocalDate birthday, String height, String weight, Float boneMineralDensity, Float bodyFatPercentage, String jwtToken) {
        String memberId = jwtUtils.accessTokenValid(jwtToken);      // jwt 토큰을 검증후 반환한 memberId
        Member member = memberRepository.findById(Long.parseLong(memberId)).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));

        if (boneMineralDensity !=null && bodyFatPercentage !=null){
            member.createAdditionalInfoNotNull(nickName,gender,birthday,height,weight,boneMineralDensity,bodyFatPercentage);
        }else
            member.createAdditionalInfoNull(nickName,gender,birthday,height,weight);

        return new TokenUserInfoResponse(TokenInfoDto.of(member.getId()));
    }

    /**
     * 회원가입시 닉네임 검증 메서드
     */
    @Override
    public void registerNickname(String nickname) {
        String redisKey = getNicknameKey(nickname);

        boolean nicknameLock = redisService.nicknameLock(redisKey, nickname, Duration.ofHours(1)); // Redis 닉네임 잠금을 시도.(데이터가 없을시)

        if (nicknameLock) {     // 잠금 성공시
            try {
                Member member = memberRepository.existsByNickname(nickname);
                if (member != null) {       // 닉네임 사용자 존재시
                    redisService.deleteValue(redisKey);
                    throw new InvalidUsageException(BAD_REQUEST, DUPLICATE_NICKNAME);
                }else       // 존재 하지 않을시 저장
                    redisService.setValues(redisKey, nickname, Duration.ofHours(1));
            } catch (Exception e) {
                redisService.deleteValue(redisKey);
                throw e;
            }
        }
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

    private String getNicknameKey(String nickname){
        return NICKNAME+nickname;
    }
}
