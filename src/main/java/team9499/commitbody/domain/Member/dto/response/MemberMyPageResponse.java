package team9499.commitbody.domain.Member.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.follow.domain.FollowType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberMyPageResponse {

    private Long memberId;     // 사용자 id
    
    private String pageType; // 마이페이지, 상대 페이지

    private String nickname;   // 사용자 닉네임

    private String profile;     // 사용자 프로필 사진

    private int followerCount;      // 팔로워 수

    private int followingCount;     // 팔로잉 수
    
    private FollowType followStatus;    // 현재 팔로우 상태
}
