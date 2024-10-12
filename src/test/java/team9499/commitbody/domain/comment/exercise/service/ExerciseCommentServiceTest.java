package team9499.commitbody.domain.comment.exercise.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.dto.ExerciseCommentDto;
import team9499.commitbody.domain.comment.exercise.dto.response.ExerciseCommentResponse;
import team9499.commitbody.domain.comment.exercise.repository.ExerciseCommentRepository;
import team9499.commitbody.domain.comment.exercise.service.impl.ExerciseCommentServiceImpl;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.redis.RedisService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ExerciseCommentServiceTest {

    @Mock private ExerciseCommentRepository exerciseCommentRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private CustomExerciseRepository customExerciseRepository;
    @Mock private RedisService redisService;

    @InjectMocks private ExerciseCommentServiceImpl exerciseCommentService;


    private Member member;
    private Exercise exercise;
    private ExerciseComment exerciseComment;
    private CustomExercise customExercise;
    private Long memberId = 1L;
    private Long exerciseId = 1L;

    @BeforeEach
    void init(){
        member = Member.builder().id(memberId).socialId("test_id").loginType(LoginType.KAKAO).nickname("사용자").build();
        exercise = new Exercise(exerciseId,"기본 운동","url", ExerciseTarget.등, ExerciseType.WEIGHT_AND_REPS, ExerciseEquipment.BAND,1.1f,new ArrayList<>());
        customExercise = CustomExercise.builder().id(exerciseId).exerciseEquipment(ExerciseEquipment.BAND).exerciseTarget(ExerciseTarget.등).customExName("커스텀 운동").member(member).build();
        exerciseComment = ExerciseComment.of(member,exercise,"운동 댓글 1");
    }

    @DisplayName("기본 운동 댓글 저장")
    @Test
    void saveDefaultExerciseComment(){
        ExerciseComment exerciseComment = ExerciseComment.of(member, exercise, "댓글 1");

        when(redisService.getMemberDto(eq(memberId.toString()))).thenReturn(Optional.of(member));
        when(exerciseRepository.findById(anyLong())).thenReturn(Optional.of(exercise));
        when(exerciseCommentRepository.save(any())).thenReturn(exerciseComment);

        exerciseCommentService.saveExerciseComment(memberId,exerciseId,"default","댓글1");

        verify(exerciseRepository,times(1)).findById(anyLong());
        verify(customExerciseRepository,times(0)).findById(anyLong());
    }

    @DisplayName("커스텀 운동 댓글 저장")
    @Test
    void saveCustomExerciseComment(){
        ExerciseComment exerciseComment = ExerciseComment.of(member, customExercise, "댓글 1");

        when(redisService.getMemberDto(eq(memberId.toString()))).thenReturn(Optional.of(member));
        when(customExerciseRepository.findById(anyLong())).thenReturn(Optional.of(customExercise));
        when(exerciseCommentRepository.save(any())).thenReturn(exerciseComment);

        exerciseCommentService.saveExerciseComment(memberId,exerciseId,"custom","댓글1");

        verify(exerciseRepository,times(0)).findById(anyLong());
        verify(customExerciseRepository,times(1)).findById(anyLong());
    }

    @DisplayName("운동 댓글 조회")
    @Test
    void getExerciseComments(){
        List<ExerciseCommentDto> exerciseCommentDtoList = new ArrayList<>();
        exerciseCommentDtoList.add(ExerciseCommentDto.builder().exerciseCommentId(1L).build());
        exerciseCommentDtoList.add(ExerciseCommentDto.builder().exerciseCommentId(2L).build());
        exerciseCommentDtoList.add(ExerciseCommentDto.builder().exerciseCommentId(3L).build());
        exerciseCommentDtoList.add(ExerciseCommentDto.builder().exerciseCommentId(4L).build());
        SliceImpl<ExerciseCommentDto> slice = new SliceImpl<>(exerciseCommentDtoList, Pageable.ofSize(10),false);

        when(exerciseCommentRepository.getExerciseComments(eq(memberId),eq(exerciseId),eq("default"),any(Pageable.class),isNull())).thenReturn(slice);

        ExerciseCommentResponse exerciseCommentResponse = exerciseCommentService.getExerciseComments(memberId, exerciseId, "default", Pageable.ofSize(10), null);
        assertThat(exerciseCommentResponse.getCommentList().size()).isEqualTo(4);
        assertThat(exerciseCommentResponse.isHasNext()).isFalse();
    }
    
    @DisplayName("운동 댓글 삭제")
    @Test
    void deleteExerciseComment(){
        when(exerciseCommentRepository.findById(anyLong())).thenReturn(Optional.of(exerciseComment));
        doNothing().when(exerciseCommentRepository).deleteByMemberIdAndId(anyLong(),anyLong());

        exerciseCommentService.deleteExerciseComment(memberId,exerciseId);

        verify(exerciseCommentRepository,times(1)).deleteByMemberIdAndId(anyLong(),anyLong());
    }
    
    @DisplayName("운동 댓글 수정")
    @Test
    void updateExerciseComment(){
        when(exerciseCommentRepository.findById(eq(exerciseId))).thenReturn(Optional.of(exerciseComment));
        
        exerciseCommentService.updateExerciseComment(memberId,exerciseId,"변경된 댓글");

        assertThat(exerciseComment.getContent()).isEqualTo("변경된 댓글");
    }

    @DisplayName("작성자가 아닐 시 발생 예외")
    @Test
    void onlyWriterException(){
        when(exerciseCommentRepository.findById(eq(exerciseId))).thenReturn(Optional.of(exerciseComment));

        assertThatThrownBy(() -> exerciseCommentService.deleteExerciseComment(3L, exerciseId))
                .isInstanceOf(InvalidUsageException.class)
                .hasMessageContaining("작성자만 이용할 수 있습니다."); // 메시지 검증도 가능
    }
}