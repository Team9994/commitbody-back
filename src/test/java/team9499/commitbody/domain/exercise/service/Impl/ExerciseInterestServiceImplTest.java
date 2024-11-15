package team9499.commitbody.domain.exercise.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.ExerciseInterest;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseInterestRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.global.redis.RedisService;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseInterestServiceImplTest {

    @Mock private ExerciseInterestRepository exerciseInterestRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private RedisService redisService;

    @InjectMocks private ExerciseInterestServiceImpl exerciseInterestService;

    private Member member;
    private Exercise exercise;
    private ExerciseInterest exerciseInterest;

    @BeforeEach
    void init(){
        member = Member.builder().id(1L).nickname("사용자").build();
        exercise = new Exercise(2L, "운동", "주소", ExerciseTarget.등, ExerciseType.TIME_ONLY, ExerciseEquipment.BAND, 1.1f, new ArrayList<>());
        exerciseInterest = ExerciseInterest.exerciseInterest(member, exercise);
    }

    @DisplayName("엘라스틱 - 관심 운동 등록")
    @Test
    void saveInterestExercise(){
        when(redisService.getMemberDto(anyString())).thenReturn(Optional.of(member));
        when(exerciseInterestRepository.findByExerciseIdAndMemberId(anyLong(),anyLong())).thenReturn(Optional.empty());
        when(exerciseRepository.findById(anyLong())).thenReturn(Optional.of(exercise));
        when(exerciseInterestRepository.save(any())).thenReturn(exerciseInterest);

        String interestStatus = exerciseInterestService.updateInterestStatus(exercise.getId(), member.getId(), "default_");
        assertThat(interestStatus).isEqualTo("등록");
    }

    @DisplayName("엘라스틱 - 관심 운동 해제")
    @Test
    void clearInterestExercise(){
        exerciseInterest.setInterested(true);
        when(redisService.getMemberDto(anyString())).thenReturn(Optional.of(member));
        when(exerciseInterestRepository.findByExerciseIdAndMemberId(anyLong(),anyLong())).thenReturn(Optional.of(exerciseInterest));

        String interestStatus = exerciseInterestService.updateInterestStatus(exercise.getId(), member.getId(), "default");
        assertThat(interestStatus).isEqualTo("해제");
    }
    
    @DisplayName("상세 조회시 관심운동 체크 - 미 좋아요시")
    @Test
    void checkFailInterestStatus(){
        when(exerciseInterestRepository.findByExerciseIdAndMemberId(anyLong(),anyLong())).thenReturn(Optional.of(exerciseInterest));

        boolean interestStatus = exerciseInterestService.checkInterestStatus(exercise.getId(), member.getId());
        assertThat(interestStatus).isFalse();
    }

    @DisplayName("상세 조회시 관심운동 체크 - 좋아요시")
    @Test
    void checkSuccessInterestStatus(){
        exerciseInterest.setInterested(true);
        when(exerciseInterestRepository.findByExerciseIdAndMemberId(anyLong(),anyLong())).thenReturn(Optional.of(exerciseInterest));

        boolean interestStatus = exerciseInterestService.checkInterestStatus(exercise.getId(), member.getId());
        assertThat(interestStatus).isTrue();
    }
}