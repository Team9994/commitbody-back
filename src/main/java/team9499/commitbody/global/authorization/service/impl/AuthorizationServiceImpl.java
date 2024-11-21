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
import team9499.commitbody.global.authorization.dto.request.LogoutRequest;
import team9499.commitbody.global.authorization.dto.response.JoinResponse;
import team9499.commitbody.global.authorization.dto.response.TokenUserInfoResponse;
import team9499.commitbody.global.authorization.repository.RefreshTokenRepository;
import team9499.commitbody.global.authorization.service.AuthorizationService;
import team9499.commitbody.global.aws.s3.S3Service;
import team9499.commitbody.global.redis.AuthType;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.global.utils.JwtUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final S3Service s3Service;
    private final JwtUtils jwtUtils;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh}")
    private int expired;        // 만료시간

    private final String ACCESS_TOKEN = "accessToken";
    private final String REFRESH_TOKEN = "refreshToken";
    private final String JOIN = "회원가입";
    private final String INCOMPLETE_JOIN = "회원가입 미완료";
    private final String LOGIN = "로그인";
    private final String RE_JOIN = "재가입";
    private final String NICKNAME = "nickname_";

    @Override
    public JoinResponse authenticateOrRegisterUser(LoginType loginType, String socialId, String fcmToken) {
        Optional<Member> optionalMember = memberRepository.findBySocialIdAndLoginType(socialId, loginType);
        return handleRegister(loginType, socialId, fcmToken, optionalMember);
    }

    /**
     * 회원가입 추가 입력
     */
    @Override
    public TokenUserInfoResponse additionalInfoSave(String nickName, Gender gender, LocalDate birthday, float height,
                                                    float weight, Float boneMineralDensity, Float bodyFatPercentage, String jwtToken) {
        Member member = validAccessTokenGetMember(jwtToken);
        saveElasticMember(nickName, member);
        createAdditionalInfo(nickName, gender, birthday, height, weight, boneMineralDensity, bodyFatPercentage, member);
        return new TokenUserInfoResponse(TokenInfoDto.of(member.getId(), member.getNickname()));
    }
    
    /**
     * 회원가입시 닉네임 검증 메서드
     */
    @Override
    public void registerNickname(String nickname) {
        String redisKey = getNicknameKey(nickname);
        if (isNicknameLock(nickname,redisKey)){
            return;
        }
        handleRegisterNickName(nickname, redisKey);
    }

    /**
     * 리프레쉬 토큰을 통한 엑시스토큰 재발급
     */
    @Override
    public Map<String, String> refreshAccessToken(String refreshToken) {
        String verifyMemberId = jwtUtils.accessTokenValid(refreshToken);
        memberRepository.findById(Long.valueOf(verifyMemberId)).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));
        return Map.of(ACCESS_TOKEN, jwtUtils.generateAccessToken(MemberDto.of(verifyMemberId)));
    }

    /**
     * 현재 로그인한 사용자를 로그아웃 합니다. : 레디스의 사용자 정보 삭제 및 MySQL 저장된 리프레쉬 토큰을 삭제합니다.

     */
    @Override
    public void logout(LogoutRequest logoutRequest) {
        handleLogout(logoutRequest.getMemberId(), logoutRequest.getJwtToken());
    }

    private void handleLogout(Long memberId, String jwtToken) {
        refreshTokenRepository.deleteByMemberId(memberId);  // 리프레쉬 토큰 삭제
        redisService.setBlackListJwt(jwtToken); // JWT토큰을 블랙리스트의 등록
        redisService.deleteValue(String.valueOf(memberId), AuthType.CERTIFICATION); // 삭제
        redisService.deleteValue(String.valueOf(memberId), AuthType.FCM);   // FCM 토큰 삭제
        SecurityContextHolder.clearContext();   // 시큐리티에 저장된 사용자 정보 삭제
    }

    /**
     * 사용자의 해당 사이트에서 회원 탈퇴를 진행합니다.
     *
     * @param memberId 현재 탈퇴할 사용자 ID
     */
    @Override
    public void withdrawMember(Long memberId, String jwtToken) {
        Member member = filterWithDrawnMember(memberId);
        member.updateWithdrawn();
        blacklistJwtAndClearMemberData(memberId, jwtToken);
    }


    private Member filterWithDrawnMember(Long memberId) {
        return memberRepository.findById(memberId).filter(member1 -> !member1.isWithdrawn()).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));
    }

    private JoinResponse handleRegister(LoginType loginType, String socialId, String fcmToken, Optional<Member> optionalMember) {
        if (optionalMember.isEmpty()) {
            return handleNewRegistration(socialId, loginType, fcmToken);
        }
        Member member = optionalMember.get();
        // 가입 미완료 회원
        if (member.getNickname() == null) {
            return handleIncompleteJoin(member, fcmToken);
        }
        // 탈퇴 회원 처리
        if (member.isWithdrawn()) {
            return handleWithdrawnMember(member, fcmToken);
        }
        return createJoinResponse(fcmToken, optionalMember.get(), LOGIN);
    }

    private JoinResponse handleNewRegistration(String socialId, LoginType loginType, String fcmToken) {
        Member newMember = memberRepository.save(Member.createSocialId(socialId, loginType, s3Service.generateRandomProfile()));
        return createJoinResponse(fcmToken, newMember, JOIN);
    }

    private JoinResponse handleIncompleteJoin(Member member, String fcmToken) {
        return createJoinResponse(fcmToken, member, INCOMPLETE_JOIN);
    }

    private JoinResponse handleWithdrawnMember(Member member, String fcmToken) {
        validRevokePeriod(member);
        member.cancelWithDrawn();
        return createJoinResponse(fcmToken, member, RE_JOIN);
    }

    private static void validRevokePeriod(Member member) {
        if (member.getWithdrawalRevokePeriod().isBefore(LocalDate.now())) {
            throw new WithDrawException("재가입 기간은 " + member.getWithdrawnAt().plusDays(1) + " 이후부터 가능합니다.");
        }
    }

    private JoinResponse createJoinResponse(String fcmToken, Member member, String loginType) {
        Map<String, String> tokens = jwtUtils.generateAuthTokens(MemberDto.builder().memberId(member.getId()).build());
        redisService.setFCM(String.valueOf(member.getId()), fcmToken); // fcm 토큰 레디스에 저장
        JoinResponse joinResponse = JoinResponse.of(loginType, TokenInfoDto.of(member), tokens.get(ACCESS_TOKEN),
                tokens.get(REFRESH_TOKEN));
        SaveRefreshToken(member.getId(), member, joinResponse.getRefreshToken());
        return joinResponse;
    }

    /*
    레디스의 정보가 서버의 문제로 인해 삭제될수있기때문에 MySQL에 리프레쉬 토큰을 저장
     */
    private void SaveRefreshToken(Long memberId, Member member, String refreshToken) {
        boolean existsByMemberId = refreshTokenRepository.existsByMemberId(memberId);
        if (!existsByMemberId) {
            refreshTokenRepository.save(
                    RefreshToken.of(member, refreshToken, LocalDateTime.now().plusMonths(expired))
            );
        }
    }

    private Member validAccessTokenGetMember(String jwtToken) {
        String memberId = jwtUtils.accessTokenValid(jwtToken);      // jwt 토큰을 검증후 반환한 memberId
        return memberRepository.findById(Long.parseLong(memberId)).filter(member1 -> !member1.isWithdrawn()).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));
    }

    private void saveElasticMember(String nickName, Member member) {
        memberDocService.saveMemberDocAsync(MemberDoc.create(member.getId(), nickName, member.getProfile()));
    }

    private void createAdditionalInfo(String nickName, Gender gender, LocalDate birthday, float height, float weight, Float boneMineralDensity, Float bodyFatPercentage, Member member) {
        if (boneMineralDensity != null && bodyFatPercentage != null) {
            member.createAdditionalInfoNotNull(nickName, gender, birthday, height, weight, boneMineralDensity, bodyFatPercentage);
            return;
        }
        member.createAdditionalInfoNull(nickName, gender, birthday, height, weight);
    }


    private String getNicknameKey(String nickname) {
        return NICKNAME + nickname;
    }

    private boolean isNicknameLock(String nickname, String redisKey) {
        // Redis 닉네임 잠금을 시도.(데이터가 없을시)
        return redisService.nicknameLock(redisKey, nickname, Duration.ofHours(1));
    }

    private void handleRegisterNickName(String nickname, String redisKey) {
        try {
            verifyAndStoreNickname(nickname, redisKey);
        } catch (Exception e) {
            redisService.deleteValue(redisKey, AuthType.CERTIFICATION);
            throw e;
        }
    }

    private void verifyAndStoreNickname(String nickname, String redisKey) {
        if (isMemberNicknamePresent(nickname)) {       // 닉네임 사용자 존재시
            redisService.deleteValue(redisKey, AuthType.CERTIFICATION);
            throw new InvalidUsageException(BAD_REQUEST, DUPLICATE_NICKNAME);
        }        // 존재 하지 않을시 저장
        redisService.setValues(redisKey, nickname, Duration.ofHours(1));
    }

    private boolean isMemberNicknamePresent(String nickname) {
        return memberRepository.findByNickname(nickname).isPresent();
    }

    private void blacklistJwtAndClearMemberData(Long memberId, String jwtToken) {
        redisService.setBlackListJwt(jwtToken);
        redisService.deleteValue(memberId.toString(), AuthType.CERTIFICATION);
        redisService.deleteValue(memberId.toString(), AuthType.FCM);
    }

}
