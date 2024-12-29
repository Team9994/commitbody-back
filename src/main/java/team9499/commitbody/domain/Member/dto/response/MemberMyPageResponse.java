package team9499.commitbody.domain.Member.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.AccountStatus;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.follow.domain.FollowType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberMyPageResponse {

    private MemberDto memberDto;

    private String pageType; // 마이페이지, 상대 페이지

    private int followerCount;      // 팔로워 수

    private int followingCount;     // 팔로잉 수
    
    private FollowType followStatus;    // 현재 팔로우 상태
    
    private boolean blockStatus;    // 차단 상태
    
    private AccountStatus accountStatus; // 계정상태

    public static MemberMyPageResponse myPageOf(Member member, String pageType,boolean blockStatus,
                                                     int followerCount, int followingCount){
      return MemberMyPageResponse.builder().memberDto(MemberDto.myPageOf(member)).pageType(pageType)
              .blockStatus(blockStatus).followerCount(followerCount).followingCount(followingCount).build();
    }

    public static MemberMyPageResponse otherPageOf(Member member, String pageType, boolean blockStatus,
                                                   int followerCount, int followingCount, FollowType followType){
        return MemberMyPageResponse.builder().memberDto(MemberDto.myPageOf(member)).pageType(pageType)
                .blockStatus(blockStatus).followerCount(followerCount).followingCount(followingCount)
                .followStatus(followType).build();
    };
}
