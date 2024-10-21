package team9499.commitbody.domain.record.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.record.domain.Record;
import team9499.commitbody.domain.record.domain.RecordDetails;
import team9499.commitbody.domain.record.domain.RecordSets;
import team9499.commitbody.domain.record.dto.RecordDto;
import team9499.commitbody.domain.record.dto.RecordSetsDto;
import team9499.commitbody.domain.record.repository.RecordDetailsRepository;
import team9499.commitbody.domain.record.repository.RecordRepository;
import team9499.commitbody.domain.record.repository.RecordSetsRepository;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.redis.RedisService;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    @Mock private RecordRepository recordRepository;
    @Mock private RecordDetailsRepository recordDetailsRepository;
    @Mock private RecordSetsRepository recordSetsRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private RecordBatchService recordBatchService;
    @Mock private RedisService redisService;

    @InjectMocks private RecordServiceImpl recordService;

    private Member member;
    private Exercise exercise1;
    private Exercise exercise2;
    private Record record;


    @BeforeEach
    void init() {
        member = Member.builder().id(1L).nickname("닉네임").isWithdrawn(false).BodyFatPercentage(10f).weight(68f)
                .height(178f).build();
        exercise1 = new Exercise(1L, "운동1", "이미지", ExerciseTarget.등, ExerciseType.WEIGHT_AND_REPS,
                ExerciseEquipment.BAND, 4f, new ArrayList<>());
        exercise2 = new Exercise(2L, "운동2", "이미지", ExerciseTarget.가슴, ExerciseType.WEIGHT_AND_REPS,
                ExerciseEquipment.DUMBBELL, 3.5f, new ArrayList<>());
        record = Record.create("기록", LocalDateTime.of(2024, 8, 10, 1, 1, 1), LocalDateTime.of(2024, 8, 10, 3, 1, 1), 2,
                member);
        record.setId(1L);

    }

    @DisplayName("루틴 저장")
    @Test
    void saveRoutine() {
        List<RecordDetails> recordDetails = new ArrayList<>();
        RecordDetails recordDetails1 = RecordDetails.create(exercise1, record, 1);
        RecordDetails recordDetails2 = RecordDetails.create(exercise1, record, 2);
        recordDetails.add(recordDetails1);
        recordDetails.add(recordDetails2);

        List<RecordSets> recordSets = new ArrayList<>();
        recordSets.add(RecordSets.ofWeightAndSets(15, 10, recordDetails1));
        recordSets.add(RecordSets.ofWeightAndSets(20, 10, recordDetails1));
        recordSets.add(RecordSets.ofWeightAndSets(30, 10, recordDetails1));
        recordSets.add(RecordSets.ofTimes(10, recordDetails2));

        // given
        when(redisService.getMemberDto(eq(member.getId().toString()))).thenReturn(Optional.of(member));
        when(exerciseRepository.findById(anyLong())).thenReturn(Optional.of(exercise1));
        when(exerciseRepository.findById(anyLong())).thenReturn(Optional.of(exercise2));

        when(recordRepository.save(any())).thenReturn(record);
        when(recordDetailsRepository.saveAll(any())).thenReturn(recordDetails);
        when(recordSetsRepository.saveAll(any())).thenReturn(recordSets);

        List<RecordSetsDto> setsList = new ArrayList<>();
        setsList.add(RecordSetsDto.builder().weight(15).reps(10).build());
        setsList.add(RecordSetsDto.builder().weight(20).reps(10).build());
        setsList.add(RecordSetsDto.builder().weight(25).reps(10).build());

        List<RecordDto> recordDtos = new ArrayList<>();
        recordDtos.add(RecordDto.builder().recordId(record.getId()).recordName("기록1").exerciseId(1L).sets(setsList)
                .endTime(LocalDateTime.now()).orders(1).source("default").build());
        // when
        Long recordId = recordService.saveRecord(member.getId(), "기록1", LocalDateTime.of(2024, 8, 10, 0, 0, 0),
                LocalDateTime.of(2024, 8, 10, 2, 0, 0), recordDtos);
        // then
        assertThat(recordId).isEqualTo(record.getId());
    }

    @DisplayName("루틴 업데이트")
    @Test
    void updateRoutine() {

        List<RecordDetails> recordDetails = new ArrayList<>();
        RecordDetails recordDetails1 = RecordDetails.create(exercise1, record, 1);
        recordDetails1.setId(1L);
        RecordDetails recordDetails2 = RecordDetails.create(exercise1, record, 2);
        recordDetails2.setId(2L);
        recordDetails.add(recordDetails1);
        recordDetails.add(recordDetails2);
        record.setDetailsList(recordDetails);

        List<RecordSets> recordSets = new ArrayList<>();
        recordSets.add(RecordSets.ofWeightAndSets(15, 10, recordDetails1));
        recordSets.add(RecordSets.ofWeightAndSets(20, 10, recordDetails1));
        recordSets.add(RecordSets.ofWeightAndSets(30, 10, recordDetails1));
        recordSets.add(RecordSets.ofTimes(10, recordDetails2));

        when(recordRepository.findByIdAndMemberId(anyLong(), anyLong())).thenReturn(record);
        when(redisService.getMemberDto(anyString())).thenReturn(Optional.of(member));
        when(recordDetailsRepository.saveAll(any())).thenReturn(recordDetails);
        when(exerciseRepository.findById(anyLong())).thenReturn(Optional.of(exercise1));
        doNothing().when(recordBatchService).insertSetsInBatch(anyList());
        doNothing().when(recordBatchService).deleteDetailsIdsInBatch(anyList());

        List<RecordSetsDto> setsList = new ArrayList<>();
        setsList.add(RecordSetsDto.builder().weight(15).reps(10).build());
        setsList.add(RecordSetsDto.builder().weight(20).reps(10).build());
        setsList.add(RecordSetsDto.builder().weight(25).reps(10).build());

        List<RecordDto> recordDtos = new ArrayList<>();
        recordDtos.add(RecordDto.builder()
                .recordId(record.getId())
                .recordName("기록1")
                .exerciseId(1L)
                .sets(setsList)
                .endTime(LocalDateTime.now())
                .orders(1)
                .source("default")
                .build());

        // Update record 호출
        recordService.updateRecord(member.getId(), record.getId(), "기록1", recordDtos);

        assertThat(record.getRecordName()).isEqualTo("기록1");
        assertThat(record.getDetailsList().size()).isEqualTo(2);
        verify(recordDetailsRepository, times(1)).saveAll(anyList());
        verify(recordBatchService, times(1)).insertSetsInBatch(anyList());
        verify(recordBatchService, times(1)).deleteDetailsIdsInBatch(anyList());
    }

    @DisplayName("기록 삭제")
    @Test
    void deleteRecord() {
        when(recordRepository.findByIdAndMemberId(anyLong(), anyLong())).thenReturn(record);
        doNothing().when(recordRepository).deleteRecord(eq(record.getId()), eq(member.getId()));

        recordService.deleteRecord(member.getId(), record.getId());

        verify(recordRepository, times(1)).deleteRecord(anyLong(), anyLong());
    }

    @DisplayName("작성자가 아닌 사용자가 삭제시 예외 발생")
    @Test
    void onlyDeleteWriterByException() {
        when(recordRepository.findByIdAndMemberId(eq(record.getId()), eq(member.getId()))).thenReturn(null);

        assertThatThrownBy(() -> recordService.deleteRecord(record.getId(), member.getId()))
                .isInstanceOf(InvalidUsageException.class).hasMessage("작성자만 이용할 수 있습니다.");
    }


}