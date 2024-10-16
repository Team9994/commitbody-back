package team9499.commitbody.domain.Member.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.global.config.QueryDslConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Import(QueryDslConfig.class)
@DataJpaTest
@ActiveProfiles("test")
class MemberRepositoryTest {

    @Autowired private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void init(){

        member = memberRepository.save(Member.builder().socialId("socialId").loginType(LoginType.KAKAO).nickname("TeSt").build());
    }

    @DisplayName("소셜아이디와 로그인 타입으로 사용자를 조회")
    @Test
    void findBySocialIdAndLoginType(){
        Optional<Member> memberOptional = memberRepository.findBySocialIdAndLoginType("socialId", LoginType.KAKAO);

        assertThat(memberOptional).isNotEmpty();
        assertThat(memberOptional).contains(member);
    }
    
    @DisplayName("닉네임으로 사용자 찾기")
    @Test
    void findByNickname(){
        Optional<Member> memberOptional = memberRepository.findByNickname("TeSt");

        assertThat(memberOptional).isNotEmpty();
        assertThat(memberOptional).contains(member);
    }
}