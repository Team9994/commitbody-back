package team9499.commitbody.global.authorization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;
import team9499.commitbody.domain.Member.domain.Gender;

import java.time.LocalDate;

@Data
public class AdditionalInfoReqeust {

    @NotBlank(message = "닉네임을 작성해주세요")
    private String nickName;
    
    @NotNull(message = "성별을 입력해주세요")
    private Gender gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Past(message = "생일은 오늘보다 이전이어야 합니다.")
    private LocalDate birthday;
    
    @NotBlank(message = "키를 입력해주세요")
    private String height;
    
    @NotBlank(message = "몸무게를 입력해주세요")
    private String weight;
    
    private Float boneMineralDensity;   // 골격근량
    
    private Float bodyFatPercentage;    //체지방량
}
