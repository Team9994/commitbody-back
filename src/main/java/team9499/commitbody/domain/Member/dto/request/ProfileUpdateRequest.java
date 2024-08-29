package team9499.commitbody.domain.Member.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.global.annotations.ValidEnum;
import team9499.commitbody.global.annotations.ValidFloat;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileUpdateRequest {

    @NotBlank(message = "변경할 닉네임을 작성해주세요")
    private String nickname;    // 닉네임

    @ValidEnum(message = "성별을 입력해주세요" ,enumClass = Gender.class)
    private Gender gender;      // 성별

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Past(message = "생일은 오늘보다 이전이어야 합니다.")
    private LocalDate birthDay; // 생년월일

    @ValidFloat(message = "변경할 키를 정확하게 입력해주세요",value="height")
    private Float height;      // 키

    @ValidFloat(message = "변경할 몸무게를 정확하게 입력해주세요",value="weight")
    private Float weight;      // 몸무게

    @ValidFloat(message = "변경할 골격근량을 정확하게 입력해주세요")
    private Float boneMineralDensity; // 골격근량

    @ValidFloat(message = "변경할 체지방량을 정확하게 입력해주세요")
    private Float bodyFatPercentage; // 체지방량

    private boolean deleteProfile; // 프로필 삭제
}
