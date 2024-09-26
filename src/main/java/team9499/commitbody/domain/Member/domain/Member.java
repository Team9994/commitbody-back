package team9499.commitbody.domain.Member.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team9499.commitbody.global.utils.BaseTime;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "member")
public class Member extends BaseTime {

    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "social_id")
    private String socialId;    // 소셜로그인 사용자 ID

    private String profile; // 사용자 프로필

    private String nickname;    // 닉네임

    private float height;      // 키

    private float weight;      // 몸무게

    private LocalDate birthday;    // 생년월일

    @Convert(converter = GenderConverter.class)
    private Gender gender;         // 성별 (남 : 0, 여 : 1)

    private String email;       // 이메일

    @Column(name = "bone_mineral_density")
    private Float BoneMineralDensity; // 골극격량

    @Column(name = "body_fat_percentage")
    private Float BodyFatPercentage; // 체지방량

    @Column(name = "notification_enabled")
    private boolean notificationEnabled; //알림 유무

    @Column(name = "is_withdrawn", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean isWithdrawn; // 사용자 회원 탈퇴 여부

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit")
    private WeightUnit weightUnit; // 무게 타입 (KG : 1, LB : 0)

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type")
    private LoginType loginType;        //로그인 타입 (KAKAO, GOOGLE)

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus;        // 계정 상태 - PUBLIC : 공개(기본 값) , PRIVATE : 비공개

    @Column(name = "withdrawn_at")
    private LocalDate withdrawnAt; // 탈퇴 만료시간

    public static Member createSocialId(String socialId,LoginType loginType,String profile){
        return Member.builder().socialId(socialId).loginType(loginType).profile(profile).accountStatus(AccountStatus.PUBLIC).build();
    }

    public void createAdditionalInfoNotNull(String nickName, Gender gender, LocalDate birthday, float height, float weight,float boneMineralDensity, float bodyFatPercentage){
        this.nickname = nickName;
        this.gender = gender;
        this.birthday = birthday;
        this.height = height;
        this.weight = weight;
        this.BoneMineralDensity = boneMineralDensity;
        this.BodyFatPercentage = bodyFatPercentage;
        this.weightUnit = WeightUnit.KG;
    }

    public void createAdditionalInfoNull(String nickName, Gender gender, LocalDate birthday, float height, float weight){
        this.nickname = nickName;
        this.gender = gender;
        this.birthday = birthday;
        this.height = height;
        this.weight = weight;
        this.weightUnit = WeightUnit.KG;
    }

    public void updateProfile(String nickname, Gender gender, LocalDate birthday, float height, float weight, Float boneMineralDensity, Float bodyFatPercentage,String profile){
        this.nickname = nickname;
        this.gender = gender;
        this.birthday = birthday;
        this.height = height;
        this.weight = weight;
        this.BoneMineralDensity = boneMineralDensity;
        this.BodyFatPercentage = bodyFatPercentage;
        if (!this.profile.equals(profile)) {
            this.profile = profile;
        }
    }

    public void updateNotification(boolean notificationEnabled){
        this.notificationEnabled = notificationEnabled;
    }

    public void updateWithdrawn(){
        this.isWithdrawn = true;
        this.withdrawnAt = LocalDate.now().plusMonths(3);
    }
}
