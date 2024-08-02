package team9499.commitbody.global.utils;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import team9499.commitbody.domain.Member.dto.MemberDto;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${jwt.access}")
    private int ACCESS_TOKEN_EXPIRED;

    @Value("${jwt.refresh}")
    private int REFRESH_TOKEN_EXPIRED;

    @Value("${jwt.secret}")
    private String SECRET;

    private final static String MEMBER_ID ="memberId";

    /**
     * AccessToken과 RefreshToken을 발급을 하는 메서드
     */
    public Map<String,String> generateAuthTokens(MemberDto memberDto){
        String accessToken = generateAccessToken(memberDto);
        String refreshToken = generateRefreshToken(memberDto);
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    /**
     * AccessToken 발급을 담당하는 메서드
     */
    public  String generateAccessToken(MemberDto memberDto) {
        LocalDateTime now = LocalDateTime.now().plusHours(ACCESS_TOKEN_EXPIRED);
        return creatToken(memberDto, Timestamp.valueOf(now));
    }

    /**
     * RefreshToken 발급을 당당하는 메서드
     */
    public  String generateRefreshToken(MemberDto memberDto) {
        LocalDateTime now = LocalDateTime.now().plusHours(REFRESH_TOKEN_EXPIRED);
        return creatToken(memberDto, Timestamp.valueOf(now));
    }

    /*
    실질적은 Token을 생성해주는 메서드
     */
    private String creatToken(MemberDto memberDto, Date expirationDate) {
        return  JWT.create()
                .withExpiresAt(expirationDate)
                .withClaim(MEMBER_ID, memberDto.getMemberId())
                .sign(Algorithm.HMAC512(SECRET));
    }
}
