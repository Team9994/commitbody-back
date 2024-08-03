package team9499.commitbody.global.authorization.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.authorization.service.OpenIdConnectService;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.global.utils.PublicKeyUtils;

import java.io.IOException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Qualifier("Kakao")
public class KakaoOpenIdConnectServiceImpl implements OpenIdConnectService {

    private final RedisService redisService;

    private final String REDIS_PUBLIC_KEY = "kakaoPublicKey";


    @Override
    public String getSocialId(String socialJwtToken) {
        String openIdURL = "https://kauth.kakao.com/.well-known/jwks.json";

        String redisPublicKey = redisService.getValue(REDIS_PUBLIC_KEY);

        RSAPublicKey rsaPublicKey = null;


        // TODO: 2024-08-02 추후 중복된 코드는 utils로 사용하는 고도화 고려
        if (redisPublicKey.equals("")) { // redis 값이 존재하지 않을때 공개키를 재 저장
            JWKSet jwkSet = loadJWKSet(openIdURL);
            List<JWK> keys = jwkSet.getKeys();

            DecodedJWT decodedJWT = JWT.decode(socialJwtToken);
            String keyId = decodedJWT.getKeyId();

            JWK jwk = keys.stream().filter(key -> key.getKeyID().equals(keyId))
                    .findFirst().orElseThrow(() -> new NoSuchException(ExceptionType.NO_SUCH_DATA));

            rsaPublicKey = getPublicKeyFromJWK(jwk);

            String publicKeyToString = PublicKeyUtils.publicKeyToString(rsaPublicKey);
            redisService.setValue(REDIS_PUBLIC_KEY, publicKeyToString);
        } else
            rsaPublicKey = PublicKeyUtils.StringToPublicKey(redisPublicKey);


        Algorithm rsa256 = Algorithm.RSA256(rsaPublicKey, null);

        DecodedJWT verify = JWT.require(rsa256).build().verify(socialJwtToken);
        return verify.getClaim("sub").asString();

    }

    private JWKSet loadJWKSet(String url) {
        try {
            return JWKSet.load(new URL(url));
        } catch (IOException | ParseException e) {
            throw new RuntimeException("JWKSet 로딩 실패: " + e.getMessage(), e);
        }
    }

    private RSAPublicKey getPublicKeyFromJWK(JWK jwk) {
        try {
            return ((RSAKey) jwk).toRSAPublicKey();
        } catch (JOSEException e) {
            throw new RuntimeException("RSA 공개키 변환 실패: " + e.getMessage(), e);
        }
    }

}
