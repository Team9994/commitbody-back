package team9499.commitbody.domain.block.servcice;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.block.domain.BlockMember;
import team9499.commitbody.domain.block.repository.BlockMemberRepository;
import team9499.commitbody.domain.block.servcice.impl.BlockMemberServiceImpl;
import team9499.commitbody.global.Exception.BlockException;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.redis.RedisService;


import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockMemberServiceTest {

    @Mock private BlockMemberRepository blockMemberRepository;
    @Mock private RedisService redisService;

    @InjectMocks private BlockMemberServiceImpl blockMemberService;

    private Long blockerId = 1L;
    private Long blockedId = 2L;

    @DisplayName("차단 해제 테스트")
    @Test
    void UnBlockMember() {
        Member blocker = Member.builder().id(blockerId).nickname("차단한 사용자").loginType(LoginType.KAKAO).build();
        Member blocked = Member.builder().id(blockedId).nickname("차단 당한 사용자").loginType(LoginType.KAKAO).build();
        BlockMember blockMember = BlockMember.of(blocker, blocked);

        when(redisService.getMemberDto(eq(blockerId.toString()))).thenReturn(Optional.of(blocker));
        when(redisService.getMemberDto(eq(blockedId.toString()))).thenReturn(Optional.of(blocked));
        when(blockMemberRepository.findByBlockerIdAndBlockedId(eq(blockerId),eq(blockedId))).thenReturn(blockMember);

        String status = blockMemberService.blockMember(blockerId, blockedId);
        
        assertThat(status).isEqualTo("차단 해제");

    }


    @DisplayName("차단 등록 테스트")
    @Test
    void BlockMember() {
        Member blocker = Member.builder().id(blockerId).nickname("차단한 사용자").loginType(LoginType.KAKAO).build();
        Member blocked = Member.builder().id(blockedId).nickname("차단 당한 사용자").loginType(LoginType.KAKAO).build();
        BlockMember blockMember = BlockMember.of(blocker, blocked);

        when(redisService.getMemberDto(eq(blockerId.toString()))).thenReturn(Optional.of(blocker));
        when(redisService.getMemberDto(eq(blockedId.toString()))).thenReturn(Optional.of(blocked));
        when(blockMemberRepository.findByBlockerIdAndBlockedId(eq(blockerId),eq(blockedId))).thenReturn(null);
        when(blockMemberRepository.save(eq(blockMember))).thenReturn(blockMember);

        String status = blockMemberService.blockMember(blockerId, blockedId);

        assertThat(status).isEqualTo("차단 성공");

    }

    
    @DisplayName("차단 상태 - 차단한 경우 예외 발생")
    @Test
    void checkBlockMember(){
        when(blockMemberRepository.checkBlock(anyLong(),anyLong())).thenThrow(new BlockException(ExceptionStatus.BAD_REQUEST, ExceptionType.BLOCK));

       assertThatThrownBy(() -> blockMemberService.checkBlock(blockerId,blockedId))
               .isInstanceOf(BlockException.class)
               .hasMessage("사용자를 차단한 상태입니다.");
    }

    @DisplayName("차단 상태 - 내가 상대방을 차단했을 경우 true 반환")
    @Test
    void checkBlockWhenIBlockedTheOtherMember() {
        when(blockMemberRepository.checkBlock(eq(blockerId), eq(blockedId))).thenReturn(Optional.empty());
        when(blockMemberRepository.checkBlock(eq(blockedId), eq(blockerId))).thenReturn(Optional.of(true));

        boolean isBlocked = blockMemberService.checkBlock(blockerId, blockedId);

        assertThat(isBlocked).isTrue();
    }


}