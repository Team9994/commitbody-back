package team9499.commitbody.global.authorization.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.global.authorization.dto.TokenInfoDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenUserInfoResponse {

    private TokenInfoDto tokenInfo;

}
