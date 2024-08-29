package team9499.commitbody.domain.Member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberMyPageResponse {

    private Long memberId;     // 사용자 id

    private String nickname;   // 사용자 닉네임

    private String profile;     // 사용자 프로필 사진

    private int followerCount;      // 팔로워 수

    private int followingCount;     // 팔로잉 수
}
