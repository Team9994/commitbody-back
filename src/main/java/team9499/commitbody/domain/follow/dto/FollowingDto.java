package team9499.commitbody.domain.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowingDto {

    private Long followId;

    private Long memberId;

    private String nickname;

    private String profile;

}
