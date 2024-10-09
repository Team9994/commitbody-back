package team9499.commitbody.domain.article.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.block.domain.BlockMember;
import team9499.commitbody.domain.block.repository.BlockMemberRepository;
import team9499.commitbody.domain.follow.domain.Follow;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.global.config.QueryDslConfig;

import static org.assertj.core.api.Assertions.*;


@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class CustomArticleRepositoryTest {


    @Autowired MemberRepository memberRepository;
    @Autowired ArticleRepository articleRepository;
    @Autowired BlockMemberRepository blockMemberRepository;
    @Autowired FollowRepository followRepository;

    private Member blocker;
    private Member followMember;
    private Pageable pageable = Pageable.ofSize(10);
    private Article article;

    @BeforeEach
    void init() {
        Member blocked = memberRepository.save(Member.builder().nickname("차단된 사용자").isWithdrawn(false).build());
        Member drawWriter = memberRepository.save(Member.builder().nickname("탈퇴한 사용자").isWithdrawn(true).build());
        followMember = memberRepository.save(Member.builder().nickname("팔로잉한 사용자").isWithdrawn(false).build());
        blocker = memberRepository.save(Member.builder().nickname("차단한 사용자").isWithdrawn(false).build());
        blockMemberRepository.save(BlockMember.of(blocker, blocked));

        followRepository.save(Follow.builder().follower(blocker).following(followMember).status(FollowStatus.FOLLOWING).build());
        followRepository.save(Follow.builder().follower(followMember).following(blocker).status(FollowStatus.REQUEST).build());

        article = articleRepository.save(Article.builder()
                .member(blocker)
                .articleType(ArticleType.INFO_QUESTION)
                .articleCategory(ArticleCategory.FEEDBACK)
                .title("차단하지 않은 게시글")
                .likeCount(15)
                .content("내용")
                .visibility(Visibility.PUBLIC)
                .build());

        articleRepository.save(Article.builder()
                .member(followMember)
                .articleType(ArticleType.INFO_QUESTION)
                .articleCategory(ArticleCategory.BODY_REVIEW)
                .title("팔로워만 공개한 게시글")
                .likeCount(22)
                .content("내용")
                .visibility(Visibility.FOLLOWERS_ONLY)
                .build());

        articleRepository.save(Article.builder()
                .member(blocker)
                .articleType(ArticleType.INFO_QUESTION)
                .articleCategory(ArticleCategory.FEEDBACK)
                .title("게시글 3")
                .likeCount(40)
                .content("내용")
                .visibility(Visibility.PUBLIC)
                .build());

        articleRepository.save(Article.builder()
                .member(blocker)
                .articleType(ArticleType.INFO_QUESTION)
                .articleCategory(ArticleCategory.BODY_REVIEW)
                .likeCount(1)
                .title("자신 비공개 게시글")
                .content("내용")
                .visibility(Visibility.PRIVATE)
                .build());

        articleRepository.save(Article.builder()
                .member(followMember)
                .articleType(ArticleType.INFO_QUESTION)
                .articleCategory(ArticleCategory.BODY_REVIEW)
                .likeCount(1)
                .title("비공개 게시글")
                .content("내용")
                .visibility(Visibility.PRIVATE)
                .build());

        articleRepository.save(Article.builder()
                .member(blocked)
                .articleType(ArticleType.INFO_QUESTION)
                .articleCategory(ArticleCategory.FEEDBACK)
                .title("차단한 사용자 게시글")
                .likeCount(0)
                .content("내용")
                .visibility(Visibility.PUBLIC)
                .build());


        articleRepository.save(Article.builder()
                .member(drawWriter)
                .articleType(ArticleType.INFO_QUESTION)
                .articleCategory(ArticleCategory.FEEDBACK)
                .title("탈퇴한 사용자의 게시글")
                .likeCount(2)
                .content("내용")
                .visibility(Visibility.PUBLIC)
                .build());
    }

    @DisplayName("운동 게시글 전제 조회")
    @Test
    void SliceArticle() {

        // 정보 질문 게시글의 모든 게시글 조회
        Slice<ArticleDto> infoArticle = articleRepository.getAllArticles(blocker.getId(), ArticleType.INFO_QUESTION, ArticleCategory.ALL, null, pageable);

        // 운동 인증 모든 게시글
        Slice<ArticleDto> exerciseArticle = articleRepository.getAllArticles(blocker.getId(), ArticleType.EXERCISE, ArticleCategory.ALL, null, pageable);

        // 운동인증 게시글의 피드백 게시글
        Slice<ArticleDto> feedBackArticle = articleRepository.getAllArticles(blocker.getId(), ArticleType.INFO_QUESTION, ArticleCategory.FEEDBACK, null, pageable);


        assertThat(infoArticle.getContent().size()).isEqualTo(3);       // 질문 게시글은 3개
        assertThat(infoArticle.hasNext()).isFalse();        // 다음 페이지는 존재하지 않음
        assertThat(exerciseArticle.getContent().isEmpty()).isTrue();    // 운동 인증 게시글은 존재하지 않음
        assertThat(feedBackArticle.getContent().size()).isEqualTo(2);   // 피드백 질믄 게시글은 2개가 존재
    }


    @DisplayName("프로필 - 작성한 게시글 조회")
    @Test
    void profileArticle() {
        Long loginMemberId = blocker.getId();

        Slice<ArticleDto> allProfileArticle = articleRepository.getAllProfileArticle(loginMemberId, loginMemberId, true, ArticleType.EXERCISE, null, pageable);
        Slice<ArticleDto> allProfileInformation = articleRepository.getAllProfileArticle(loginMemberId, loginMemberId, true, ArticleType.INFO_QUESTION, null, pageable);
        Slice<ArticleDto> otherProfileInformation = articleRepository.getAllProfileArticle(loginMemberId, followMember.getId(), false, ArticleType.INFO_QUESTION, null, pageable);

        assertThat(allProfileArticle.getContent().isEmpty()).isTrue();
        assertThat(allProfileArticle.hasNext()).isFalse();
        assertThat(allProfileInformation.getContent().size()).isEqualTo(3);
        assertThat(otherProfileInformation.getContent().isEmpty()).isTrue();
    }

    @DisplayName("상세 게시글 조회")
    @Test
    void articleDetail() {
        ArticleDto detailArticle = articleRepository.getDetailArticle(blocker.getId(), article.getId());

        assertThat(detailArticle.getTitle()).isEqualTo("차단하지 않은 게시글");
        assertThat(detailArticle.getContent()).isEqualTo("내용");
    }
    
    @DisplayName("게시글 삭제")
    @Test
    void deleteByArticle(){
        articleRepository.deleteArticle(article.getId());

        boolean deleteStatus = articleRepository.existsById(article.getId());
        assertThat(deleteStatus).isFalse(); //존재하지 않는 게시글
    }
}


