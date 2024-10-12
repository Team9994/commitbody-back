package team9499.commitbody.domain.comment.exercise.repository;

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
import team9499.commitbody.domain.block.domain.BlockMember;
import team9499.commitbody.domain.block.repository.BlockMemberRepository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.dto.ExerciseCommentDto;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.like.domain.ContentLike;
import team9499.commitbody.domain.like.repository.LikeRepository;
import team9499.commitbody.global.config.QueryDslConfig;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment.*;
import static team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget.*;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class ExerciseCommentRepositoryTest {

    @Autowired private ExerciseCommentRepository exerciseCommentRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private BlockMemberRepository blockMemberRepository;
    @Autowired private LikeRepository likeRepository;
    @Autowired private MemberRepository memberRepository;

    private ExerciseComment exerciseComment;
    private Member member;
    private Exercise exercise;
    private List<ExerciseComment> exerciseCommentList;



    @BeforeEach
    void init(){
        exercise = exerciseRepository.save(new Exercise(1L,"운동","http://test.com",등, ExerciseType.WEIGHT_AND_REPS,BAND,1.1f,new ArrayList<>()));
        member = memberRepository.save(Member.builder().nickname("사용자1").loginType(LoginType.KAKAO).socialId("test_id1").build());
        Member blocked = memberRepository.save(Member.builder().nickname("사용자2").loginType(LoginType.KAKAO).socialId("test_id2").build());
        Member withDrawn = memberRepository.save(Member.builder().nickname("사용자3").loginType(LoginType.KAKAO).socialId("test_id3").isWithdrawn(true).build());

        blockMemberRepository.save(BlockMember.of(member,blocked));
        exerciseComment = exerciseCommentRepository.save(ExerciseComment.of(member,exercise,"운동 댓글1"));


        List<ContentLike> contentLikes = List.of(likeRepository.save(ContentLike.createLike(member, exerciseComment)));
        exerciseComment.setExerciseCommentLikes(contentLikes);

        List<ExerciseComment> exerciseComments = new ArrayList<>();
        exerciseComments.add(ExerciseComment.builder().content("운동 댓글2").likeStatus(false).likeCount(0).member(member).exercise(exercise).exerciseCommentLikes(new ArrayList<>()).build());
        exerciseComments.add(ExerciseComment.builder().content("탈퇴한 사용자 댓글").likeStatus(false).likeCount(0).member(withDrawn).exercise(exercise).exerciseCommentLikes(new ArrayList<>()).build());
        exerciseComments.add( ExerciseComment.builder().content("차단한 사용자 댓글").likeStatus(false).likeCount(0).member(blocked).exercise(exercise).exerciseCommentLikes(new ArrayList<>()).build());
        exerciseComments.add(ExerciseComment.builder().content("탈퇴한 사용자 댓글").likeStatus(false).likeCount(0).member(withDrawn).exercise(exercise).exerciseCommentLikes(new ArrayList<>()).build());
        exerciseComments.add(ExerciseComment.builder().content("운동 댓글3").likeStatus(false).likeCount(0).member(member).exercise(exercise).exerciseCommentLikes(new ArrayList<>()).build());

        exerciseCommentList = exerciseCommentRepository.saveAll(exerciseComments);
    }

    @DisplayName("운동 댓글 삭제")
    @Test
    void deleteByExerciseComment(){
        List<ExerciseComment> before = exerciseCommentRepository.findAll();
        exerciseCommentRepository.deleteByMemberIdAndId(member.getId(),exerciseComment.getId());

        List<ExerciseComment> after = exerciseCommentRepository.findAll();
        assertThat(before.size()).isEqualTo(6);
        assertThat(after.size()).isEqualTo(5);
    }
    
    @DisplayName("운동 댓글 무한 스크롤 조회")
    @Test
    void getAllExerciseComment(){

        Slice<ExerciseCommentDto> all = exerciseCommentRepository.getExerciseComments(member.getId(), exercise.getId(), "default", Pageable.ofSize(10), null);
        Slice<ExerciseCommentDto> before = exerciseCommentRepository.getExerciseComments(member.getId(), exercise.getId(), "default", Pageable.ofSize(2), null);
        Slice<ExerciseCommentDto> after = exerciseCommentRepository.getExerciseComments(member.getId(), exercise.getId(), "default", Pageable.ofSize(2), exerciseCommentList.get(0).getId());

        assertThat(all.getContent().size()).isEqualTo(3);
        assertThat(all.hasNext()).isFalse();
        assertThat(before.getContent().size()).isEqualTo(2);
        assertThat(before.hasNext()).isTrue();
        assertThat(after.getContent().size()).isEqualTo(1);
        assertThat(after.hasNext()).isFalse();
    }
}