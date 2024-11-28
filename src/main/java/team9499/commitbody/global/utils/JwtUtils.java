package team9499.commitbody.global.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.global.Exception.JwtTokenException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import static team9499.commitbody.global.Exception.ExceptionStatus.*;
import static team9499.commitbody.global.Exception.ExceptionType.*;

@Slf4j
@Component
public class JwtUtils {

    private final static String MEMBER_ID = "memberId";
    private final static String ACCESS_TOKEN = "accessToken";
    private final static String REFRESH_TOKEN = "refreshToken";

    @Value("${jwt.access}")
    private int ACCESS_TOKEN_EXPIRED;
    @Value("${jwt.refresh}")
    private int REFRESH_TOKEN_EXPIRED;
    @Value("${jwt.secret}")
    private String SECRET;

    /**
     * AccessToken과 RefreshToken을 발급을 하는 메서드
     */
    public Map<String, String> generateAuthTokens(MemberDto memberDto) {
        String accessToken = generateAccessToken(memberDto);
        String refreshToken = generateRefreshToken(memberDto);
        return Map.of(ACCESS_TOKEN, accessToken, REFRESH_TOKEN, refreshToken);
    }

    /**
     * AccessToken 발급을 담당하는 메서드
     */
    public String generateAccessToken(MemberDto memberDto) {
        LocalDateTime now = LocalDateTime.now().plusMinutes(ACCESS_TOKEN_EXPIRED);
        return creatToken(memberDto, Timestamp.valueOf(now));
    }

    /**
     * RefreshToken 발급을 당당하는 메서드
     */
    public String generateRefreshToken(MemberDto memberDto) {
        LocalDateTime now = LocalDateTime.now().plusMonths(REFRESH_TOKEN_EXPIRED);
        return creatToken(memberDto, Timestamp.valueOf(now));
    }

    public String accessTokenValid(String jwtToken) {
        try {
            if (!isTokenExpired(jwtToken)) {
                return verifyToken(jwtToken);
            }
            throw new JwtTokenException(UNAUTHORIZED, TOKEN_EXPIRED);
        } catch (SignatureVerificationException e) {
            log.error("존재하지 않은 토큰 사용");
            throw new JwtTokenException(UNAUTHORIZED, TOKEN_NOT_FOUND);
        }
    }

    /*
    토큰의 대한 시간 검증 (true : 토큰 만료 , false : 토큰 유효)
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expired = JWT.decode(token).getExpiresAt();
            return expired != null && expired.before(new Date());
        } catch (JWTDecodeException e) {
            throw new JwtTokenException(BAD_REQUEST, INVALID_TOKEN_MESSAGE);
        }
    }

    /*
    토큰의 정보를 추출하는 메서드
     */
    public String verifyToken(String tokenValue) {
        try {
            return jwtVerify(tokenValue);
        } catch (AlgorithmMismatchException e) {
            throw new JwtTokenException(BAD_REQUEST, TOKEN_NOT_FOUND);
        }
    }

    /*
     실질적은 Token을 생성해주는 메서드
    */
    private String creatToken(MemberDto memberDto, Date expirationDate) {
        return JWT.create()
                .withExpiresAt(expirationDate)
                .withClaim(MEMBER_ID, String.valueOf(memberDto.getMemberId()))
                .sign(Algorithm.HMAC512(SECRET));
    }

    private String jwtVerify(String tokenValue) {
        return JWT.require(Algorithm.HMAC512(SECRET)).build()
                .verify(tokenValue)
                .getClaim(MEMBER_ID)
                .asString();
    }
}
