package team9499.commitbody.global.authorization.service;

import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.global.authorization.dto.request.LogoutRequest;
import team9499.commitbody.global.authorization.dto.response.JoinResponse;
import team9499.commitbody.global.authorization.dto.response.TokenUserInfoResponse;

import java.time.LocalDate;
import java.util.Map;

public interface AuthorizationService {

    JoinResponse authenticateOrRegisterUser(LoginType loginType, String socialId, String fcmToken);

    TokenUserInfoResponse additionalInfoSave(String nickName, Gender gender, LocalDate birthday, float height,
                                             float weight, Float boneMineralDensity, Float bodyFatPercentage,
                                             String jwtToken);

    void registerNickname(String nickname,Long memberId);

    Map<String,String> refreshAccessToken(String refreshToken);

    void logout(LogoutRequest logoutRequest);

    void withdrawMember(Long memberId,String jwtToken);
}
