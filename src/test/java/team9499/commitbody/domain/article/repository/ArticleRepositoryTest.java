package team9499.commitbody.domain.article.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
import team9499.commitbody.domain.block.domain.BlockMember;
import team9499.commitbody.domain.block.repository.BlockMemberRepository;
import team9499.commitbody.global.config.DataDBConfig;
import team9499.commitbody.global.config.QueryDslConfig;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(transactionManager = "dataTransactionManager")
@Import({QueryDslConfig.class, DataDBConfig.class})
@ActiveProfiles("test")
class ArticleRepositoryTest {

    @Autowired ArticleRepository articleRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired BlockMemberRepository blockMemberRepository;

    private Member blocker;

    @BeforeEach
    void init() {
        Member blocked = memberRepository.save(Member.builder().nickname("blocked").build());
        Member drawWriter = memberRepository.save(Member.builder().nickname("blocked").isWithdrawn(true).build());
        blocker = memberRepository.save(Member.builder().nickname("blocker").build());
        blockMemberRepository.save(BlockMember.of(blocker, blocked));

        articleRepository.save(Article.builder()
                .member(blocker)
                .articleType(ArticleType.INFO_QUESTION)
                .title("차단하지 않은 게시글")
                .content("내용")
                .visibility(Visibility.PUBLIC)
                .build());

        articleRepository.save(Article.builder()
                .member(blocked)
                .articleType(ArticleType.INFO_QUESTION)
                .title("차단한 사용자 게시글")
                .content("내용")
                .visibility(Visibility.PUBLIC)
                .build());


        articleRepository.save(Article.builder()
                .member(drawWriter)
                .articleType(ArticleType.INFO_QUESTION)
                .title("탈퇴한 사용자의 게시글")
                .content("내용")
                .visibility(Visibility.PUBLIC)
                .build());

    }

    @DisplayName("인기 게시글 (NativeQuery) 조회 쿼리 _ Row()함수 사용")
    @Test
    void popularArticleAll() {
        List<Article> all = articleRepository.findAll();
        List<Object[]> byPopularArticle = articleRepository.findByPopularArticle(blocker.getId());

        assertThat(all.size()).isEqualTo(3);
        assertThat(byPopularArticle.size()).isEqualTo(1);
        assertThat(byPopularArticle.get(0)[8]).isEqualTo("차단하지 않은 게시글");
    }
}