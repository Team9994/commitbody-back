package team9499.commitbody.domain.exercise.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseInterestRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.exercise.service.ExerciseInterestService;
import team9499.commitbody.domain.like.repository.LikeRepository;
import team9499.commitbody.domain.record.repository.RecordRepository;
import team9499.commitbody.global.aws.s3.S3Service;
import team9499.commitbody.global.redis.RedisService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@Slf4j
@ExtendWith(MockitoExtension.class)
class ExerciseServiceImplTest {

    @Mock private CustomExerciseRepository customExerciseRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private RecordRepository recordRepository;
    @Mock private ExerciseInterestRepository exerciseInterestRepository;
    @Mock private ExerciseInterestService exerciseInterestService;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private RedisService redisService;
    @Mock private MemberRepository memberRepository;
    @Mock private S3Service s3Service;

    @InjectMocks
    private ExerciseServiceImpl exerciseService;

    private Member member;
    private CustomExercise customExercise;
    private MockMultipartFile file;

    @BeforeEach
    void init() {
        member = Member.builder().id(1l).nickname("사용자").build();
        customExercise = CustomExercise.builder().id(2L).customExName("커스텀 운동").customGifUrl("test.png")
                .exerciseTarget(ExerciseTarget.등).exerciseEquipment(ExerciseEquipment.BAND).member(member).build();
        file = new MockMultipartFile("test", "test.png", "image/png", "test".getBytes(StandardCharsets.UTF_8));
    }

    @DisplayName("커스텀 운동 등록")
    @Test
    void customExerciseSave() {

        when(s3Service.uploadFile(eq(file))).thenReturn("test.png");
        when(redisService.getMemberDto(anyString())).thenReturn(Optional.of(member));
        when(customExerciseRepository.save(any())).thenReturn(customExercise);

        CustomExerciseDto customExerciseDto = exerciseService.saveCustomExercise("커스텀 운동", ExerciseTarget.등, ExerciseEquipment.BAND, member.getId(), file);

        assertThat(customExerciseDto.getExerciseName()).isEqualTo(customExercise.getCustomExName());
        assertThat(customExerciseDto.getExerciseEquipment()).isEqualTo(customExercise.getExerciseEquipment());
    }
    
    @DisplayName("커스텀 운동 업데이트")
    @Test
    void updateCustomExercise(){
        when(customExerciseRepository.findByIdAndAndMemberId(anyLong(),anyLong())).thenReturn(Optional.of(customExercise));

        CustomExerciseDto customExerciseDto = exerciseService.updateCustomExercise("변경된 운동", ExerciseTarget.가슴, ExerciseEquipment.BARBELL, member.getId(), customExercise.getId(), file);
        assertThat(customExercise.getCustomExName()).isEqualTo(customExerciseDto.getExerciseName());
    }
    
    @DisplayName("커스텀 운동 삭제시 관련 데이터 삭제")
    @Test
    void deleteCustomExercise(){
        when(customExerciseRepository.findByIdAndAndMemberId(anyLong(),anyLong())).thenReturn(Optional.of(customExercise));

        exerciseService.deleteCustomExercise(customExercise.getId(),member.getId());

        verify(recordRepository,times(1)).deleteCustomExercise(anyLong());
        verify(exerciseInterestRepository,times(1)).deleteAllByCustomExerciseId(anyLong());
        verify(likeRepository,times(1)).deleteByCustomExerciseId(anyLong());
        verify(customExerciseRepository,times(1)).delete(any());
    }

    @DisplayName("상세운동 조회")
    @Test
    void detailExercise(){

    }


}