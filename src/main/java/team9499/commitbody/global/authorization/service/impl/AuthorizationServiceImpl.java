package team9499.commitbody.global.authorization.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.domain.MemberDoc;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.Member.service.MemberDocService;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.Exception.WithDrawException;
import team9499.commitbody.global.authorization.domain.RefreshToken;
import team9499.commitbody.global.authorization.dto.TokenInfoDto;
import team9499.commitbody.global.authorization.dto.response.TokenUserInfoResponse;
import team9499.commitbody.global.authorization.repository.RefreshTokenRepository;
import team9499.commitbody.global.authorization.service.AuthorizationElsService;
import team9499.commitbody.global.authorization.service.AuthorizationService;
import team9499.commitbody.global.redis.AuthType;
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
@Transactional(transactionManager = "dataTransactionManager")
public class AuthorizationServiceImpl implements AuthorizationService {

    private final RedisService redisService;
    private final MemberRepository memberRepository;
    private final MemberDocService memberDocService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthorizationElsService authorizationElsService;
    @Value("${jwt.refresh}")
    private int expired;        // 만료시간

    @Value("${default.profile}")
    private String profileUrl;

    private final String REFRESH_TOKEN = "refreshToken";
    private final String JOIN = "회원가입";
    private final String LOGIN = "로그인";
    private final String RE_JOIN = "재가입";
    private final String NICKNAME ="nickname_";

    @Override
    public Map<String,Object> authenticateOrRegisterUser(LoginType loginType,String socialId,String fcmToken) {
        String joinOrLogin = "";
        Optional<Member> optionalMember = memberRepository.findBySocialIdAndLoginType(socialId,loginType);

        if (!optionalMember.isPresent()){
            Member member = memberRepository.save(Member.createSocialId(socialId,loginType,profileUrl));
            optionalMember = Optional.of(member);
            joinOrLogin = JOIN;
        }
        else{       //로그인인 경우
            Member member = optionalMember.get();
            if (member.isWithdrawn()&&member.getWithdrawalRevokePeriod().isBefore(LocalDate.now())){    // 재가입은 일정 기간 이후에 가능
                throw new WithDrawException("재가입 기간은 "+member.getWithdrawnAt().plusDays(1)+" 이후부터 가능합니다.");
            }else if (member.isWithdrawn()){        // 일정 기간내에 가입할 경우 가입 취소
                member.cancelWithDrawn();
                joinOrLogin = RE_JOIN;
            }else{
                joinOrLogin = LOGIN;
            }
        }

        Map<String, Object> tokenMap = new LinkedHashMap<>(jwtUtils.generateAuthTokens(MemberDto.builder().memberId(optionalMember.get().getId()).build()));
        tokenMap.put("authMode",joinOrLogin);       // 로그인 / 회원가입을 구분하기위함

        redisService.setFCM(String.valueOf(optionalMember.get().getId()),fcmToken);     // fcm 토큰 레디스에 저장

        if (joinOrLogin.equals(LOGIN) || joinOrLogin.equals(RE_JOIN)) tokenMap.put("tokenInfo",TokenInfoDto.of(optionalMember.get().getId(),optionalMember.get().getNickname()));     //로그인일 경우에만 JWT토큰의대한 정보를 담는다
        SaveRefreshToken(optionalMember.get().getId(), optionalMember, (String) tokenMap.get(REFRESH_TOKEN));
        return tokenMap;
    }

    @Override
    public TokenUserInfoResponse additionalInfoSave(String nickName, Gender gender, LocalDate birthday, float height, float weight, Float boneMineralDensity, Float bodyFatPercentage, String jwtToken) {
        String memberId = jwtUtils.accessTokenValid(jwtToken);      // jwt 토큰을 검증후 반환한 memberId
        Member member = memberRepository.findById(Long.parseLong(memberId)).filter(member1 -> !member1.isWithdrawn()).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));

        memberDocService.saveMemberDocAsync(MemberDoc.create(member.getId(),nickName,member.getProfile()));

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
                    redisService.deleteValue(redisKey,AuthType.CERTIFICATION);
                    throw new InvalidUsageException(BAD_REQUEST, DUPLICATE_NICKNAME);
                }else       // 존재 하지 않을시 저장
                    redisService.setValues(redisKey, nickname, Duration.ofHours(1));
            } catch (Exception e) {
                redisService.deleteValue(redisKey,AuthType.CERTIFICATION);
                throw e;
            }
        }
    }

    /**
     * 리프레쉬 토큰을 통한 엑시스토큰 재발급
     */
    @Override
    public Map<String,String> refreshAccessToken(String refreshToken) {
        String verifyMemberId = jwtUtils.accessTokenValid(refreshToken);
        memberRepository.findById(Long.valueOf(verifyMemberId)).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));

        String newAccessToken = jwtUtils.generateAccessToken(MemberDto.builder().memberId(Long.valueOf(verifyMemberId)).build());
        return Map.of("accessToken",newAccessToken);
    }

    /**
     * 현재 로그인한 사용자를 로그아웃 합니다. : 레디스의 사용자 정보 삭제 및 MySQL 저장된 리프레쉬 토큰을 삭제합니다.
     * @param memberId  로그인한 사용자 ID
     * @param jwtToken 현재 사용한 JWT 토큰
     */
    @Override
    public void logout(Long memberId, String jwtToken) {
        refreshTokenRepository.deleteByMemberId(memberId);  // 리프레쉬 토큰 삭제
        redisService.setBlackListJwt(jwtToken); // JWT토큰을 블랙리스트의 등록
        redisService.deleteValue(String.valueOf(memberId), AuthType.CERTIFICATION); // 삭제
        redisService.deleteValue(String.valueOf(memberId), AuthType.FCM);   // FCM 토큰 삭제
        SecurityContextHolder.clearContext();   // 시큐리티에 저장된 사용자 정보 삭제
    }

    /**
     * 사용자의 해당 사이트에서 회원 탈퇴를 진행합니다.
     * @param memberId 현재 탈퇴할 사용자 ID
     */
    @Override
    public void withdrawMember(Long memberId, String jwtToken) {
        Member member = memberRepository.findById(memberId).filter(member1 -> !member1.isWithdrawn()).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));
        member.updateWithdrawn();
        redisService.setBlackListJwt(jwtToken);
        redisService.deleteValue(memberId.toString(),AuthType.CERTIFICATION);
        redisService.deleteValue(memberId.toString(),AuthType.FCM);
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
