package team9499.commitbody.global.authorization.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokenInfoDto {

    private Long memberId;

    private String nickname;

    public TokenInfoDto(Long memberId) {
        this.memberId = memberId;
    }

    private TokenInfoDto(Long memberId, String nickname) {
        this.memberId = memberId;
        this.nickname = nickname;
    }

    public static TokenInfoDto of(Long memberId,String nickname){
        return new TokenInfoDto(memberId,nickname);
    }

    public static TokenInfoDto of(Long memberId){
        return new TokenInfoDto(memberId);
    }
}
