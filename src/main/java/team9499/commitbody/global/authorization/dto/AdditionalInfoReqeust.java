package team9499.commitbody.global.authorization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;
import team9499.commitbody.domain.Member.domain.Gender;

import java.time.LocalDate;

@Data
@Schema(name = "추가정보 입력 Request")
public class AdditionalInfoReqeust {

    @Schema(description = "사용자 닉네임")
    @NotBlank(message = "닉네임을 작성해주세요")
    private String nickName;
    
    @Schema(description = "FEMALE : 여성, MALE : 남성")
    @NotNull(message = "성별을 입력해주세요")
    private Gender gender;

    @Schema(description = "생년월일의 형식은 2024-08-03(yyyy-MM-dd) 형식으로 보내야하며, 가입일 기준 일부터 가입이 가능")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Past(message = "생일은 오늘보다 이전이어야 합니다.")
    private LocalDate birthday;

    @Schema(description = "사용자 키")
    @NotBlank(message = "키를 입력해주세요")
    private String height;

    @Schema(description = "사용자 몸무게")
    @NotBlank(message = "몸무게를 입력해주세요")
    private String weight;

    @Schema(description = "필수값이 아닙니다.")
    private Float boneMineralDensity;   // 골격근량

    @Schema(description = "필수값이 아닙니다.")
    private Float bodyFatPercentage;    //체지방량
}
