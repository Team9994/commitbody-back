package team9499.commitbody.domain.follow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FollowDto {

    private Long followId;

    private Long memberId;

    private String nickname;

    private String profile;

    private Boolean followStatus;    //팔로우 상태

    private Boolean isCurrentUser;  // 현재 사용자를 나태냄

    public FollowDto(Long followId, Long memberId, String nickname, String profile, boolean followStatus) {
        this.followId = followId;
        this.memberId = memberId;
        this.nickname = nickname;
        this.profile = profile;
        this.followStatus = followStatus;
    }
}
