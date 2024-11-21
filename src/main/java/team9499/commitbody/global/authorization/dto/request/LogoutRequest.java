package team9499.commitbody.global.authorization.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogoutRequest {

    private Long memberId;
    private String jwtToken;
}
