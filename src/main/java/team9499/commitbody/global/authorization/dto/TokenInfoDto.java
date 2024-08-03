package team9499.commitbody.global.authorization.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokenInfoDto {

    private Long memberId;

    private TokenInfoDto(Long memberId) {
        this.memberId = memberId;
    }

    public static TokenInfoDto of(Long memberId){
        return new TokenInfoDto(memberId);
    }
}
