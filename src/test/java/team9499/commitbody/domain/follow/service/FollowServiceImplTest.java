package team9499.commitbody.domain.follow.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.follow.domain.Follow;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.follow.domain.FollowType;
import team9499.commitbody.domain.follow.dto.FollowDto;
import team9499.commitbody.domain.follow.dto.response.FollowResponse;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.global.redis.RedisService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static team9499.commitbody.domain.follow.domain.FollowStatus.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock private FollowRepository followRepository;
    @Mock private RedisService redisService;

    @InjectMocks private FollowServiceImpl followService;


    private Long followerId = 1L;
    private Long followingId = 2L;
    private Member memberFollower;
    private Member memberFollowing;

    private final String MUTUAL_FOLLOW = "맞팔로우";
    private final String UNFOLLOW = "언팔로우";
    private final String CANCEL_FOLLOW = "팔로우 취소";
    private final String REQUEST_FOLLOW = "팔로우 요청";

    @BeforeEach
    void init(){
        memberFollower = Member.builder().id(followerId).nickname("사용자1").isWithdrawn(false).build();
        memberFollowing = Member.builder().id(followingId).nickname("사용자2").isWithdrawn(false).build();

    }

    @DisplayName("팔로워 - 팔로잉 요청")
    @Test
    void followRequest() {
        whenMember();

        Follow follower = createFollow(memberFollower,memberFollowing,FOLLOWING);
        Follow following= createFollow(memberFollowing,memberFollower,REQUEST);

        //type =Follow 일때
        when(followRepository.findByFollowerIdAndFollowingId(eq(followerId),eq(followingId))).thenReturn(Optional.empty());
        //존재하지 않기때문에 follow 요청
        when(followRepository.save(any())).thenReturn(follower);

        when(followRepository.findByFollowerIdAndFollowingId(eq(followingId),eq(followerId))).thenReturn(Optional.empty());
        when(followRepository.save(any())).thenReturn(following);

        String follow = followService.follow(followerId, followingId, FollowType.FOLLOW);

        assertThat(follow).isEqualTo(REQUEST_FOLLOW);
        assertThat(follower.getStatus()).isEqualTo(FollowStatus.FOLLOWING);
        assertThat(following.getStatus()).isEqualTo(FollowStatus.REQUEST);
    }

    @DisplayName("팔로워 - 맞팔로워")
    @Test
    void followMutualFollow() {
        whenMember();

        Follow follower = Follow.builder().follower(memberFollower).following(memberFollowing).status(FOLLOWING).build();
        Follow following= Follow.builder().follower(memberFollowing).following(memberFollower).status(REQUEST).build();

        when(followRepository.findByFollowerIdAndFollowingId(eq(followerId),eq(followingId))).thenReturn(Optional.of(follower));
        when(followRepository.findByFollowerIdAndFollowingId(eq(followingId),eq(followerId))).thenReturn(Optional.of(following));

        String follow = followService.follow(followingId, followerId, FollowType.FOLLOW);

        assertThat(follow).isEqualTo(MUTUAL_FOLLOW);
        assertThat(follower.getStatus()).isEqualTo(FollowStatus.MUTUAL_FOLLOW);
        assertThat(following.getStatus()).isEqualTo(FollowStatus.MUTUAL_FOLLOW);
    }

    @DisplayName("팔로워 - 언팔로워(맞팔로워 상태일경우)")
    @Test
    void followUnFollow() {
        whenMember();

        Follow follower = createFollow(memberFollower,memberFollowing,FollowStatus.MUTUAL_FOLLOW);
        Follow following= createFollow(memberFollowing,memberFollower,FollowStatus.MUTUAL_FOLLOW);

        when(followRepository.findByFollowerIdAndFollowingId(eq(followerId),eq(followingId))).thenReturn(Optional.of(follower));
        when(followRepository.findByFollowerIdAndFollowingId(eq(followingId),eq(followerId))).thenReturn(Optional.of(following));

        String follow = followService.follow(followingId, followerId, FollowType.UNFOLLOW);
        assertThat(follow).isEqualTo(UNFOLLOW);
        assertThat(follower.getStatus()).isEqualTo(FollowStatus.FOLLOWING);
        assertThat(following.getStatus()).isEqualTo(FollowStatus.UNFOLLOW);
    }
    
    @DisplayName("팔로워 - 취소(한쪽만 팔로워일 경우)")
    @Test
    void followCancel() {
        whenMember();

        Follow follower = createFollow(memberFollower,memberFollowing,FOLLOWING);
        Follow following= createFollow(memberFollowing,memberFollower,REQUEST);

        when(followRepository.findByFollowerIdAndFollowingId(eq(followerId),eq(followingId))).thenReturn(Optional.of(follower));
        when(followRepository.findByFollowerIdAndFollowingId(eq(followingId),eq(followerId))).thenReturn(Optional.of(following));

        String follow = followService.follow(followerId, followingId, FollowType.UNFOLLOW);

        assertThat(follow).isEqualTo(CANCEL_FOLLOW);
        assertThat(follower.getStatus()).isEqualTo(CANCEL);
        assertThat(following.getStatus()).isEqualTo(CANCEL);
    }


    @DisplayName("팔로워 목록 조회")
    @Test
    void getFollowers(){
        List<FollowDto> followDtos = new ArrayList<>();
        followDtos.add(FollowDto.of(followerId,2L,"사용자2","test",false));
        followDtos.add(FollowDto.of(followerId,3L,"사용자3","test",false));
        followDtos.add(FollowDto.of(followerId,4L,"사용자4","test",false));
        followDtos.add(FollowDto.of(followerId,5L,"사용자5","test",false));

        SliceImpl<FollowDto> dtoSlice = new SliceImpl<>(followDtos, Pageable.ofSize(10), false);

        when(followRepository.getAllFollowers(anyLong(),anyLong(),eq("사용"),isNull(),any(Pageable.class))).thenReturn(dtoSlice);

        FollowResponse followResponse = followService.getFollowers(followingId, followerId, "사용", null, Pageable.ofSize(10));
        
        assertThat(followResponse.isHasNext()).isFalse();
        assertThat(followResponse.getFollows().size()).isEqualTo(4);
        assertThat(followResponse.getFollows()).containsAll(followDtos);
    }
    
    
    @DisplayName("사용자 차단시 팔로워 상태 변경")
    @Test
    void BlockCancelFollow(){
        doNothing().when(followRepository).cancelFollow(anyLong(),anyLong());

        followService.cancelFollow(followerId,followingId,"차단 성공");

        verify(followRepository,times(1)).cancelFollow(anyLong(),anyLong());
    }


    private void whenMember() {
        when(redisService.getMemberDto(eq(followerId.toString()))).thenReturn(Optional.of(memberFollower));
        when(redisService.getMemberDto(eq(followingId.toString()))).thenReturn(Optional.of(memberFollowing));
    }

    private Follow createFollow(Member follower, Member following,FollowStatus followStatus){
        return Follow.builder().follower(follower).following(following).status(followStatus).build();
    }
}