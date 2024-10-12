package team9499.commitbody.domain.comment.article.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.block.domain.BlockMember;
import team9499.commitbody.domain.block.repository.BlockMemberRepository;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;
import team9499.commitbody.global.config.QueryDslConfig;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static team9499.commitbody.domain.article.domain.ArticleCategory.*;
import static team9499.commitbody.domain.article.domain.ArticleType.*;
import static team9499.commitbody.domain.article.domain.Visibility.*;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class CustomArticleCommentRepositoryTest {

    @Autowired private ArticleCommentRepository articleCommentRepository;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private BlockMemberRepository blockMemberRepository;

    private Article article;
    private Member writer;
    private Member member;
    private Member withDrawMember;
    @BeforeEach
    void init(){
        writer = memberRepository.save(Member.builder().nickname("작성자").loginType(LoginType.KAKAO).socialId("Kakao_id").build());
        withDrawMember = memberRepository.save(Member.builder().nickname("탈퇴한 사용자").loginType(LoginType.KAKAO).socialId("Kakao_id1").isWithdrawn(true).build());
        member = memberRepository.save(Member.builder().nickname("사용자1").loginType(LoginType.KAKAO).socialId("Kakao_id2").build());
        article = articleRepository.save(Article.of("제목","내용", INFO_QUESTION, FEEDBACK, PUBLIC,writer));
    }

    @DisplayName("게시글의 작성된 모든 댓글 조회 - 최신순 조회")
    @Test
    void getAllCommentByArticleOrderByRecent(){
        List<ArticleComment> articleCommentList = new ArrayList<>();

        articleCommentList.add(ArticleComment.builder().article(article).member(writer).content("댓글1").parent(null).childComments(new ArrayList<>()).build());
        articleCommentList.add(ArticleComment.builder().article(article).member(withDrawMember).content("댓글2").childComments(new ArrayList<>()).build());   // 탈퇴한 사용자의 댓글
        articleCommentList.add(ArticleComment.builder().article(article).member(member).content("댓글3").childComments(new ArrayList<>()).build());

        articleCommentRepository.saveAll(articleCommentList);

        Slice<ArticleCommentDto> allCommentByArticleALL = articleCommentRepository.getAllCommentByArticle(article.getId(), writer.getId(), null, null, OrderType.RECENT, Pageable.ofSize(10));
        Slice<ArticleCommentDto> allCommentByArticlePer = articleCommentRepository.getAllCommentByArticle(article.getId(), writer.getId(), null, null, OrderType.RECENT, Pageable.ofSize(1));
        Slice<ArticleCommentDto> allCommentByArticleNext = articleCommentRepository.getAllCommentByArticle(article.getId(), writer.getId(), articleCommentList.get(2).getId(), null, OrderType.RECENT, Pageable.ofSize(1));

        assertThat(allCommentByArticleALL.getContent().size()).isEqualTo(2);
        assertThat(allCommentByArticlePer.getContent().size()).isEqualTo(1);
        assertThat(allCommentByArticlePer.getContent().get(0).getContent()).isEqualTo("댓글3");
        assertThat(allCommentByArticlePer.getContent().get(0).isWriter()).isFalse(); // 비 작성자
        assertThat(allCommentByArticlePer.hasNext()).isTrue();
        assertThat(allCommentByArticleNext.hasNext()).isFalse();
        assertThat(allCommentByArticleNext.getContent().get(0).getContent()).isEqualTo("댓글1");
        assertThat(allCommentByArticleNext.getContent().get(0).isWriter()).isTrue();    // 작성자
    }

    @DisplayName("게시글의 작성된 모든 댓글 조회 - 좋아요순으로 조회")
    @Test
    void getAllCommentByArticleOrderByLike(){
        List<ArticleComment> articleCommentList = new ArrayList<>();

        articleCommentList.add(ArticleComment.builder().article(article).member(writer).content("댓글1").parent(null).likeCount(2).childComments(new ArrayList<>()).build());
        articleCommentList.add(ArticleComment.builder().article(article).member(writer).content("댓글2").parent(null).likeCount(20).childComments(new ArrayList<>()).build());
        articleCommentList.add(ArticleComment.builder().article(article).member(withDrawMember).content("댓글3").likeCount(40).childComments(new ArrayList<>()).build());   // 탈퇴한 사용자
        articleCommentList.add(ArticleComment.builder().article(article).member(member).content("댓글4").likeCount(50).childComments(new ArrayList<>()).build());   // 차단당한 사용자

        articleCommentRepository.saveAll(articleCommentList);
        blockMemberRepository.save(BlockMember.of(writer,member));  // 사용자 차단

        Slice<ArticleCommentDto> allCommentByArticleALL = articleCommentRepository.getAllCommentByArticle(article.getId(), writer.getId(), null, null, OrderType.LIKE, Pageable.ofSize(10));
        Slice<ArticleCommentDto> allCommentByArticlePer = articleCommentRepository.getAllCommentByArticle(article.getId(), writer.getId(), null, null, OrderType.LIKE, Pageable.ofSize(1));
        Slice<ArticleCommentDto> allCommentByArticleNext = articleCommentRepository.getAllCommentByArticle(article.getId(), writer.getId(), articleCommentList.get(1).getId(), null, OrderType.LIKE, Pageable.ofSize(1));

        assertThat(allCommentByArticleALL.getContent().size()).isEqualTo(2);
        assertThat(allCommentByArticleALL.getContent().get(0).isWriter()).isTrue();
        assertThat(allCommentByArticleALL.getContent().get(0).getContent()).isEqualTo("댓글2");
        assertThat(allCommentByArticleALL.getContent().get(1).isWriter()).isTrue();
        assertThat(allCommentByArticleALL.getContent().get(1).getContent()).isEqualTo("댓글1");
        assertThat(allCommentByArticlePer.hasNext()).isTrue();
        assertThat(allCommentByArticleNext.hasNext()).isFalse();
    }

    @DisplayName("게시글의 작성된 댓글수 조회")
    @Test
    void getCommentCount(){
        List<ArticleComment> articleCommentList = new ArrayList<>();

        articleCommentList.add(ArticleComment.builder().article(article).member(writer).content("댓글1").parent(null).likeCount(2).childComments(new ArrayList<>()).build());
        articleCommentList.add(ArticleComment.builder().article(article).member(writer).content("댓글2").parent(null).likeCount(20).childComments(new ArrayList<>()).build());
        articleCommentList.add(ArticleComment.builder().article(article).member(withDrawMember).content("댓글3").likeCount(40).childComments(new ArrayList<>()).build());   // 탈퇴한 사용자
        articleCommentList.add(ArticleComment.builder().article(article).member(member).content("댓글4").likeCount(50).childComments(new ArrayList<>()).build());   // 차단당한 사용자

        articleCommentRepository.saveAll(articleCommentList);
        blockMemberRepository.save(BlockMember.of(writer,member));  // 사용자 차단

        Integer commentCount = articleCommentRepository.getCommentCount(article.getId(), writer.getId());

        assertThat(commentCount).isEqualTo(2);
    }
    
    @DisplayName("댓글의 달린 대댓글 모두 조회")
    @Test
    void getAllReplyComment(){

        ArticleComment articleComment = articleCommentRepository.save(ArticleComment.of(article, writer, "댓글", null));
        List<ArticleComment> articleReplyCommentList = new ArrayList<>();

        articleReplyCommentList.add(ArticleComment.builder().article(article).member(writer).content("대댓글1").parent(articleComment).likeCount(2).childComments(new ArrayList<>()).build());
        articleReplyCommentList.add(ArticleComment.builder().article(article).member(writer).content("대댓글2").parent(articleComment).likeCount(20).childComments(new ArrayList<>()).build());
        articleReplyCommentList.add(ArticleComment.builder().article(article).member(withDrawMember).content("대댓글3").parent(articleComment).likeCount(40).childComments(new ArrayList<>()).build());   // 탈퇴한 사용자
        articleReplyCommentList.add((ArticleComment.builder().article(article).member(member).content("대댓글4").parent(articleComment).likeCount(50).childComments(new ArrayList<>()).build()));

        articleCommentRepository.saveAll(articleReplyCommentList);

        Slice<ArticleCommentDto> allReplyComments = articleCommentRepository.getAllReplyComments(articleComment.getId(), writer.getId(), null, Pageable.ofSize(10));
        Slice<ArticleCommentDto> allReplyCommentsNext = articleCommentRepository.getAllReplyComments(articleComment.getId(), writer.getId(), articleReplyCommentList.get(3).getId(), Pageable.ofSize(1));

        assertThat(allReplyComments.getContent().size()).isEqualTo(3);
        assertThat(allReplyComments.getContent()).doesNotContain(ArticleCommentDto.builder().commentId(articleReplyCommentList.get(2).getId()).build());
        assertThat(allReplyCommentsNext.hasNext()).isTrue();
    }
    
    @DisplayName("부모 댓글의 작성된 자식 댓글 ID 모두 조회")
    @Test
    void articleCommentReplyBYIds(){
        ArticleComment articleComment = articleCommentRepository.save(ArticleComment.of(article, writer, "댓글", null));
        List<ArticleComment> articleReplyCommentList = new ArrayList<>();

        articleReplyCommentList.add(ArticleComment.builder().article(article).member(writer).content("대댓글1").parent(articleComment).likeCount(2).childComments(new ArrayList<>()).build());
        articleReplyCommentList.add(ArticleComment.builder().article(article).member(writer).content("대댓글2").parent(articleComment).likeCount(20).childComments(new ArrayList<>()).build());
        articleReplyCommentList.add(ArticleComment.builder().article(article).member(withDrawMember).content("대댓글3").parent(articleComment).likeCount(40).childComments(new ArrayList<>()).build());   // 탈퇴한 사용자
        articleReplyCommentList.add((ArticleComment.builder().article(article).member(member).content("대댓글4").parent(articleComment).likeCount(50).childComments(new ArrayList<>()).build()));

        articleCommentRepository.saveAll(articleReplyCommentList);

        List<Long> allChildComment = articleCommentRepository.getAllChildComment(articleComment.getId());

        List<Long> ids = List.of(articleReplyCommentList.get(0).getId(),articleReplyCommentList.get(1).getId(),articleReplyCommentList.get(3).getId());
        assertThat(allChildComment.size()).isEqualTo(3);
        assertThat(allChildComment).doesNotContain(articleReplyCommentList.get(2).getId());
        assertThat(allChildComment).containsAll(ids);
    }
    
    @DisplayName("탈퇴한 사용자가 작성한 댓글의 게시글 ID 조회")
    @Test
    void findCommentArticleIdByMemberId(){

        Article article2 = articleRepository.save(Article.of("제목", "내용", INFO_QUESTION, FEEDBACK, PUBLIC, writer));
        Article article3 = articleRepository.save(Article.of("제목", "내용", INFO_QUESTION, FEEDBACK, PUBLIC, writer));
        Article article4 = articleRepository.save(Article.of("제목", "내용", INFO_QUESTION, FEEDBACK, PUBLIC, member));

        articleCommentRepository.save(ArticleComment.of(article, writer, "댓글", null));
        articleCommentRepository.save(ArticleComment.of(article2, writer, "댓글", null));
        articleCommentRepository.save(ArticleComment.of(article3, writer, "댓글", null));
        articleCommentRepository.save(ArticleComment.of(article3, member, "댓글", null));

        List<Long> commentArticleIdsByMemberId = articleCommentRepository.findCommentArticleIdsByMemberId(writer.getId());

        assertThat(commentArticleIdsByMemberId.size()).isEqualTo(3);
        assertThat(commentArticleIdsByMemberId).doesNotContain(article4.getId());

    }
}