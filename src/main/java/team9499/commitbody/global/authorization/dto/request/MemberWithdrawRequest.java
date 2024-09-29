package team9499.commitbody.global.authorization.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberWithdrawRequest {

    @AssertTrue(message = "약관에 동의해야 합니다.")
    private Boolean check;
}
