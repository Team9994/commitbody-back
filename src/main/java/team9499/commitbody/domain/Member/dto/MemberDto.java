package team9499.commitbody.domain.Member.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.LoginType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberDto {

    private Long memberId;

    private String socialId;    // 소셜로그인 사용자 ID

    private String nickname;    // 닉네임

    private String height;      // 키

    private String weight;      // 몸무게

    private String birthday;    // 생년월일

    private Integer sex;         // 성별 (남 : 0, 여 : 1)

    private String email;       // 이메일

    private float BoneMineralDensity; // 골극격량

    private float BodyFatPercentage; // 체지방량

    private boolean notificationEnabled; //알림 유무

    private Integer weightUnit; // 무게 타입 (KG : 1, LB : 0)

    private LoginType loginType;        //로그인 타입 (KAKAO, GOOGLE)

    private boolean isUserDeactivated; //알림유무(true : 알림 받기, false : 알림 안받기)


}
