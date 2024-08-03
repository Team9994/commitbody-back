package team9499.commitbody.global.utils;

import lombok.extern.slf4j.Slf4j;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.ServerException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class PublicKeyUtils {

    /**
     * 공개키를 인코딩해서 String 타입으로 변환한다.
     * @param rsaPublicKey  발급받은 공개키
     */
    public static String publicKeyToString(RSAPublicKey rsaPublicKey){
        byte[] publicKeyEncoded = rsaPublicKey.getEncoded();
        return Base64.getEncoder().encodeToString(publicKeyEncoded);
    }

    /**
     * 인코딩된 키의 String 값을 다시 사용 할 수 있는 RSA 공개키로 변환하는 메서드
     * @param encodeKey 저장된 String 타입의 공개키 정보
     * @return
     */
    public static RSAPublicKey StringToPublicKey(String encodeKey){
        byte[] decodeKey = Base64.getDecoder().decode(encodeKey);

        KeyFactory keyFactory = createKeyFactory("RSA");

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodeKey);

        return createPublicKey(keyFactory,keySpec);
    }


    /*
    공개키로 변환하는 메서드
     */
    private static RSAPublicKey createPublicKey(KeyFactory keyFactory,X509EncodedKeySpec keySpec){
        try {
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            log.error("공개키 생성 도중 오류 발생");
            throw new ServerException(ExceptionType.SERVER_ERROR);
        }
    }
    
    /*
    임의의 키의 값을 실제 사용될수 있는 공개키로 변환하는 메서드
     */
    private static KeyFactory createKeyFactory(String type){
        try {
            return KeyFactory.getInstance(type);
        } catch (NoSuchAlgorithmException e) {
            log.error("공개키 변환 과정중 오류 발생");
            throw new ServerException(ExceptionType.SERVER_ERROR);
        }
    }
}
