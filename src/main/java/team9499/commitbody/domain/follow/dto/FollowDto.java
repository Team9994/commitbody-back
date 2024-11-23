package team9499.commitbody.domain.follow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
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

    public static FollowDto of(Long followId, Long memberId, String nickname, String profile, boolean followStatus) {
        return FollowDto.builder().followId(followId).memberId(memberId).nickname(nickname)
                .profile(profile).followStatus(followStatus).build();
    }

    public static FollowDto of(Long followId, Long memberId, String nickname, String profile,
                               Boolean followStatus, Boolean isCurrentUser){
        return FollowDto.builder().followId(followId).memberId(memberId).nickname(nickname)
                .profile(profile).followStatus(followStatus).isCurrentUser(isCurrentUser).build();
    }
}
