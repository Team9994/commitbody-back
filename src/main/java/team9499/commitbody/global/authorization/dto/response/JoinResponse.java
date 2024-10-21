package team9499.commitbody.global.authorization.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import team9499.commitbody.global.authorization.dto.TokenInfoDto;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class JoinResponse {

    private String accessToken;
    private String refreshToken;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String authMode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private TokenInfoDto tokenInfoDto;

    public static JoinResponse of(String loginType, TokenInfoDto tokenInfoDto, String accessToken, String refreshToken){
        return JoinResponse.builder().authMode(loginType).tokenInfoDto(tokenInfoDto).accessToken(accessToken).refreshToken(refreshToken).build();
    }
}
