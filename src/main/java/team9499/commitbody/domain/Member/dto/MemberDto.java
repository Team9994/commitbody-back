package team9499.commitbody.domain.Member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.domain.WeightUnit;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberDto {

    private Long memberId;

    private String socialId;    // 소셜로그인 사용자 ID

    private String nickname;    // 닉네임
    
    private String profile;     // 사용자 프로필

    private String height;      // 키

    private String weight;      // 몸무게

    private LocalDate birthday;    // 생년월일

    private Gender gender;         // 성별(MALE, FEMALE)

    private String email;       // 이메일

    private Float BoneMineralDensity; // 골극격량

    private Float BodyFatPercentage; // 체지방량

    private WeightUnit weightUnit; // 무게 타입 (KG, LB)

    private LoginType loginType;        //로그인 타입 (KAKAO, GOOGLE)

    private Boolean blockStatus;    // 차단 상태

    @JsonIgnore
    private boolean notificationEnabled; //알림 유무

    @JsonIgnore
    private boolean isUserDeactivated; //알림유무(true : 알림 받기, false : 알림 안받기)


    public static MemberDto toMemberDTO(Member member){
        return MemberDto.builder()
                .memberId(member.getId()).nickname(member.getNickname()).build();
    }

    public static MemberDto createNickname(Long memberId,String nickname, String profile){
        return MemberDto.builder().profile(profile).memberId(memberId).nickname(nickname).build();
    }
}
