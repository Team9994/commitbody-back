package team9499.commitbody.domain.block.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.block.domain.BlockMember;
import team9499.commitbody.global.config.QueryDslConfig;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class BlockMemberRepositoryTest {

    @Autowired private BlockMemberRepository blockMemberRepository;
    @Autowired private MemberRepository memberRepository;


    private Long blockerId;
    private Long blockedId;

    @BeforeEach
    void init(){
        Member blocker = memberRepository.save(Member.builder().nickname("차단한 사용자").socialId("1231151").loginType(LoginType.KAKAO).build());
        Member blocked = memberRepository.save(Member.builder().nickname("차단 당한 사용자").socialId("66666").loginType(LoginType.KAKAO).build());
        blockMemberRepository.save(BlockMember.of(blocker,blocked));

        blockerId = blocker.getId();
        blockedId = blocked.getId();
    }

    @DisplayName("차단된 사용자 조회")
    @Test
    void findByBlockMemberById(){

        BlockMember blockMember = blockMemberRepository.findByBlockerIdAndBlockedId(blockerId, blockedId);

        assertThat(blockMember.getBlocker().getNickname()).isEqualTo("차단한 사용자");
        assertThat(blockMember.getBlocked().getNickname()).isEqualTo("차단 당한 사용자");
    }
    
    @DisplayName("사용자를 차단했는제 체크")
    @Test
    void checkBlockMember(){

        Boolean falseBlock = blockMemberRepository.checkBlock(blockerId, blockerId).orElse(false);
        Boolean trueBlock = blockMemberRepository.checkBlock(blockerId, blockedId).orElse(false);

        assertThat(falseBlock).isFalse();
        assertThat(trueBlock).isTrue();
    }

}