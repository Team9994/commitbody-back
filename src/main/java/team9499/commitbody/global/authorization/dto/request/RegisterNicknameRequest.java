package team9499.commitbody.global.authorization.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterNicknameRequest {

    @Pattern( regexp = "^(?:(?=.*[a-zA-Z])(?=.*[가-힣])[a-zA-Z가-힣0-9]{3,8}|(?=.*[a-zA-Z])[a-zA-Z0-9]{3,8}|(?=.*[가-힣])[가-힣0-9]{3,8})$", message = "닉네임 형식이 맞게 작성해 주세요")
    private String nickname;
}
