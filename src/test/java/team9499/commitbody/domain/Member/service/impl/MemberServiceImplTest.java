package team9499.commitbody.domain.Member.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.Member.domain.Gender;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.response.MemberMyPageResponse;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.Member.service.MemberDocService;
import team9499.commitbody.domain.article.service.ElsArticleService;
import team9499.commitbody.domain.block.servcice.BlockMemberService;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.global.Exception.BlockException;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.aws.s3.S3Service;
import team9499.commitbody.global.redis.RedisService;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock private MemberRepository memberRepository;
    @Mock private FollowRepository followRepository;
    @Mock private BlockMemberService blockMemberService;
    @Mock private MemberDocService memberDocService;
    @Mock private ElsArticleService elsArticleService;
    @Mock private RedisService redisService;
    @Mock private S3Service s3Service;

    @InjectMocks private MemberServiceImpl memberService;

    private Long memberId = 1L;
    private Member member;
    private Member otherMember;

    @BeforeEach
    void init(){
        member = Member.builder().id(memberId).nickname("my").isWithdrawn(false).socialId("testId").profile("other.jpeg").loginType(LoginType.KAKAO).notificationEnabled(false).build();
        otherMember = Member.builder().id(2L).nickname("other").isWithdrawn(false).socialId("otherId").profile("default.jpeg").loginType(LoginType.KAKAO).notificationEnabled(false).build();
    }
    
    @DisplayName("마이 페이지 조회 - 자신 페이지 조회시")
    @Test
    void getMyPage(){
        when(memberRepository.findByNickname(eq(member.getNickname()))).thenReturn(Optional.of(member));
        when(blockMemberService.checkBlock(anyLong(),anyLong())).thenReturn(false);
        when(followRepository.getCountFollower(eq(memberId))).thenReturn(10L);
        when(followRepository.getCountFollowing(eq(memberId))).thenReturn(5L);

        MemberMyPageResponse response = memberService.getMyPage(memberId, member.getNickname());
        assertThat(response.getPageType()).isEqualTo("myPage");
        assertThat(response.getNickname()).isEqualTo("my");
    }
    
    @DisplayName("마이 페이지 조회 - 상대방 페이이 조회시")
    @Test
    void getOtherPage(){
        when(memberRepository.findByNickname(eq(otherMember.getNickname()))).thenReturn(Optional.of(otherMember));
        when(blockMemberService.checkBlock(anyLong(),anyLong())).thenReturn(false);
        when(followRepository.getCountFollower(eq(otherMember.getId()))).thenReturn(10L);
        when(followRepository.getCountFollowing(eq(otherMember.getId()))).thenReturn(5L);
        when(followRepository.followStatus(eq(memberId),eq(otherMember.getId()))).thenReturn(null);

        MemberMyPageResponse response = memberService.getMyPage(memberId, otherMember.getNickname());
        assertThat(response.getNickname()).isEqualTo(otherMember.getNickname());
        assertThat(response.getPageType()).isEqualTo("theirPage");
        assertThat(response.isBlockStatus()).isFalse();
    }

    @DisplayName("마이 페이지 조회 - 상대방이 날 차단한 경우 예외")
    @Test
    void otherMemberBlockException(){
        when(memberRepository.findByNickname(eq(otherMember.getNickname()))).thenReturn(Optional.of(otherMember));
        when(blockMemberService.checkBlock(eq(otherMember.getId()),eq(memberId))).thenThrow(new BlockException(ExceptionStatus.BAD_REQUEST, ExceptionType.BLOCK));

        assertThatThrownBy(() -> memberService.getMyPage(memberId,otherMember.getNickname())).hasMessage("사용자를 차단한 상태입니다.").isInstanceOf(BlockException.class);
    }

    @DisplayName("사용자 프로필 업데이트")
    @Test
    void updateMemberProfile(){
        String defaultProfile = "default.jpeg";
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
        when(s3Service.updateProfile(isNull(),eq(member.getProfile()),eq(true))).thenReturn(defaultProfile);
        doNothing().when(redisService).updateMember(eq(memberId.toString()),eq(member));
        doNothing().when(memberDocService).updateMemberDocAsync(anyString(),anyString(),anyString());
        doNothing().when(elsArticleService).updateWriterAsync(anyString(),anyString());

        memberService.updateProfile(memberId,"update my", Gender.MALE, LocalDate.of(1999,8,28),1.1f,14.2f,14f,12f,true,null);

        assertThat(member.getNickname()).isEqualTo("update my");
        assertThat(member.getProfile()).isEqualTo(defaultProfile);
        verify(redisService,times(1)).updateMember(anyString(),any());
        verify(memberDocService,times(1)).updateMemberDocAsync(anyString(),anyString(),anyString());
        verify(elsArticleService,times(1)).updateWriterAsync(anyString(),anyString());
    }
    
    @DisplayName("알림 수신 여부 조회")
    @Test
    void checkNotification(){
        when(redisService.getMemberDto(eq(memberId.toString()))).thenReturn(Optional.of(member));

        boolean notification = memberService.getNotification(memberId);
        assertThat(notification).isFalse();
    }

    
    @DisplayName("알림 수신 여부 업데이트")
    @Test
    void updateNotification(){
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
        doNothing().when(redisService).updateMember(anyString(),any());

        String notification = memberService.updateNotification(memberId);
        assertThat(notification).isEqualTo("알림 수신");
        assertThat(member.isNotificationEnabled()).isTrue();

    }
}