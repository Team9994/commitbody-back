package team9499.commitbody.global.authorization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import team9499.commitbody.domain.Member.domain.LoginType;

@Schema(name = "로그인 및 회원가입 Request")
@Data
public class JoinLoginRequest {

    @Schema(description = "로그인시 진행했던 소셜로그인 타입")
    private LoginType loginType;

    @Schema(description = "회원가입시 필요한 소셜로그인 Id")
    private String socialId;
}
