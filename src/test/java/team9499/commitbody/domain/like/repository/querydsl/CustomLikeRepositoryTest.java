package team9499.commitbody.domain.like.repository.querydsl;

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
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.comment.article.repository.ArticleCommentRepository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.repository.ExerciseCommentRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.like.domain.ContentLike;
import team9499.commitbody.domain.like.repository.LikeRepository;
import team9499.commitbody.global.config.QueryDslConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class CustomLikeRepositoryTest {

    @Autowired private ExerciseCommentRepository exerciseCommentRepository;
    @Autowired private CustomExerciseRepository customExerciseRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ArticleCommentRepository articleCommentRepository;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private LikeRepository likeRepository;
    
    
    private Member member;
    private Member diffMember;

    @BeforeEach
    void init(){
        member = memberRepository.save(Member.builder().nickname("사용자").loginType(LoginType.KAKAO).build());
        diffMember = memberRepository.save(Member.builder().nickname("사용자1").loginType(LoginType.KAKAO).build());
    }
    
    @DisplayName("커스텀 운동 좋아요 삭제 테스트")
    @Test
    void deleteExerciseLike(){
        CustomExercise customExercise = customExerciseRepository.save(CustomExercise.builder().exerciseTarget(ExerciseTarget.등).exerciseEquipment(ExerciseEquipment.BAND).customExName("커스텀 운동").build());
        ExerciseComment exerciseComment = ExerciseComment.builder().member(member).customExercise(customExercise).likeStatus(false).exerciseCommentLikes(new ArrayList<>()).build();
        exerciseCommentRepository.save(exerciseComment);
        likeRepository.save(ContentLike.createLike(member, exerciseComment));

        likeRepository.deleteByCustomExerciseId(customExercise.getId());

        List<ContentLike> all = likeRepository.findAll();
        assertThat(all).isEmpty();
    }
    
    
    @DisplayName("사용자가 좋아한 타인 게시글 ID 리스트 조회")
    @Test
    void memberArticleLikeIds(){
        Article article1 = articleRepository.save(Article.builder().member(diffMember).title("게시글1").build());
        Article article2 = articleRepository.save(Article.builder().member(member).title("게시글2").build());
        Article article3 = articleRepository.save(Article.builder().member(diffMember).title("게시글3").build());
        articleRepository.save(Article.builder().member(member).title("게시글4").build());
        
        // 2개의 게시글 좋아요 상태
        likeRepository.save(ContentLike.builder().member(member).article(article1).likeStatus(true).build());
        likeRepository.save(ContentLike.builder().member(member).article(article3).likeStatus(true).build());
        likeRepository.save(ContentLike.builder().member(member).article(article2).likeStatus(true).build());   // 자신 게시글이라 제외

        List<Long> articleIdsByDeleteMember = likeRepository.findArticleIdsByDeleteMember(member.getId());

        assertThat(articleIdsByDeleteMember).containsAll(List.of(article1.getId(),article3.getId()));
        assertThat(articleIdsByDeleteMember.size()).isEqualTo(2);
    }
    
    @DisplayName("사용자가 운동 댓글의 좋아요 조회")
    @Test
    void findByMemberIdAndExerciseCommentId(){
        Exercise exercise = exerciseRepository.save(new Exercise(1L, "기본 운동", "URL", ExerciseTarget.등, ExerciseType.TIME_ONLY, ExerciseEquipment.BAND, 1.1f, new ArrayList<>()));
        ExerciseComment exerciseComment = exerciseCommentRepository.save(ExerciseComment.of(member, exercise, "기본 운동 댓글1"));

        Optional<ContentLike> contentLikeEmpty = likeRepository.findByMemberIdAndExerciseCommentId(member.getId(), exerciseComment.getId());

        likeRepository.save(ContentLike.createLike(member, exerciseComment));
        Optional<ContentLike> contentLikeNotEmpty = likeRepository.findByMemberIdAndExerciseCommentId(member.getId(), exerciseComment.getId());

        assertThat(contentLikeEmpty).isEmpty();
        assertThat(contentLikeNotEmpty).isNotEmpty();
        assertThat(contentLikeNotEmpty.get().isLikeStatus()).isTrue();
    }
    
    @DisplayName("게시글 좋아요 조회")
    @Test
    void findByMemberIdAndArticleIdAndArticleCommentIdIsNull(){
        Article article = articleRepository.save(Article.builder().title("게시글1").build());
        likeRepository.save(ContentLike.createArticleLike(member,article));
        Optional<ContentLike> contentLike = likeRepository.findByMemberIdAndArticleIdAndArticleCommentIdIsNull(member.getId(), article.getId());

        assertThat(contentLike).isNotEmpty();
        assertThat(contentLike.get().getArticle()).isEqualTo(article);
        assertThat(contentLike.get().isLikeStatus()).isTrue();

    }
    
    @DisplayName("게시글 댓글 좋아요 조회")
    @Test
    void findByMemberIdAndArticleCommentId(){
        Article article = articleRepository.save(Article.builder().title("게시글1").build());
        ArticleComment parentComment = articleCommentRepository.save(ArticleComment.builder().member(member).parent(null).article(article).build());
        likeRepository.save(ContentLike.creatArticleCommentLike(member, parentComment));

        Optional<ContentLike> contentLike = likeRepository.findByMemberIdAndArticleCommentId(member.getId(), parentComment.getId());
        assertThat(contentLike).isNotEmpty();
        assertThat(contentLike.get().isLikeStatus()).isEqualTo(true);
        assertThat(contentLike.get().getArticleComment()).isEqualTo(parentComment);

    }
}