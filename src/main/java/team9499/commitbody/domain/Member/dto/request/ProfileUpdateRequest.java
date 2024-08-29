package team9499.commitbody.domain.Member.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "프로핑 수정 Request", description = "프로플 수정에 사용되는 Request")
public class ProfileUpdateRequest {

    @Schema(description = "변경할 닉네임 명")
    @NotBlank(message = "변경할 닉네임을 작성해주세요")
    private String nickname;    // 닉네임

    @Schema(description = "변경할 성별[MALE, FEMALE]")
    @ValidEnum(message = "성별을 입력해주세요" ,enumClass = Gender.class)
    private Gender gender;      // 성별

    @Schema(description = "변경할 생년월일 금일 날짜보다 이전 이어야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Past(message = "생일은 오늘보다 이전이어야 합니다.")
    private LocalDate birthDay; // 생년월일

    @Schema(description = "변경할 키 입니다. 81 ~299cm 까지 사용 가능합니다.")
    @ValidFloat(message = "변경할 키를 정확하게 입력해주세요",value="height")
    private Float height;      // 키

    @Schema(description = "변경할 몸무게 입니다. 11~199kg 까지 사용 가능합니다.")
    @ValidFloat(message = "변경할 몸무게를 정확하게 입력해주세요",value="weight")
    private Float weight;      // 몸무게

    @Schema(description = "변경할 골격근량 입니다. 1~99까지 사용가능합니다.")
    @ValidFloat(message = "변경할 골격근량을 정확하게 입력해주세요")
    private Float boneMineralDensity; // 골격근량

    @Schema(description = "변경할 체지방량 입니다. 1~99까지 사용가능합니다.")
    @ValidFloat(message = "변경할 체지방량을 정확하게 입력해주세요")
    private Float bodyFatPercentage; // 체지방량

    @Schema(description = "기본 프로필로 사용 할 경우 true를 사용하며, 아닐시 false를 사용합니다.")
    private boolean deleteProfile; // 프로필 삭제
}
