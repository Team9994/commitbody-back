package team9499.commitbody.global.authorization.service;

import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.global.authorization.dto.response.TokenUserInfoResponse;

import java.time.LocalDate;
import java.util.Map;

public interface AuthorizationService {

    Map<String,Object> authenticateOrRegisterUser(LoginType loginType,String socialId,String fcmToken);

    TokenUserInfoResponse additionalInfoSave(String nickName, Gender gender, LocalDate birthday, float height, float weight, Float boneMineralDensity, Float bodyFatPercentage, String jwtToken);

    void registerNickname(String nickname);

    Map<String,String> refreshAccessToken(String refreshToken);

    void logout(Long memberId, String jwtToken);

    void withdrawMember(Long memberId,String jwtToken);
}
