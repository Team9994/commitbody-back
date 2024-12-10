package team9499.commitbody.global.authorization.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.Member.service.MemberDocService;
import team9499.commitbody.global.authorization.dto.response.JoinResponse;
import team9499.commitbody.global.authorization.repository.RefreshTokenRepository;
import team9499.commitbody.global.aws.s3.S3Service;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.global.utils.JwtUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock private RedisService redisService;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberDocService memberDocService;
    @Mock private S3Service s3Service;
    @Mock private JwtUtils jwtUtils;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks AuthorizationServiceImpl authorizationService;

    private Member member;
    private Member drawMember;

    @BeforeEach
    void init(){
        member = Member.builder().id(1L).isWithdrawn(false).socialId("social-id1").loginType(LoginType.KAKAO).nickname("사용자1").build();
        drawMember = Member.builder().id(2L).isWithdrawn(true).socialId("social-id2").loginType(LoginType.GOOGLE).nickname("탈퇴 사용자").build();
    }

    @Test
    void authenticateOrRegisterUser(){
        Map<String,String> map = Map.of("accessToken","accessToken", "refreshToken","refreshToken");

        when(memberRepository.findBySocialIdAndLoginType(anyString(),eq(LoginType.KAKAO))).thenReturn(Optional.empty());
        when(memberRepository.save(any())).thenReturn(member);
        when(jwtUtils.generateAuthTokens(any())).thenReturn(map);
        when(refreshTokenRepository.existsByMemberId(anyLong())).thenReturn(true);
        when(s3Service.generateRandomProfile()).thenReturn("test");
        doNothing().when(redisService).setFCM(anyString(),anyString());

        JoinResponse joinResponse = authorizationService.authenticateOrRegisterUser(LoginType.KAKAO, "social-id1", "fcmToken");

        log.info("join ={}", joinResponse);

    }

}