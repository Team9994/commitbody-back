package team9499.commitbody.domain.record.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.record.domain.Record;
import team9499.commitbody.domain.record.domain.RecordDetails;
import team9499.commitbody.domain.record.domain.RecordSets;
import team9499.commitbody.domain.record.dto.response.RecordResponse;
import team9499.commitbody.global.config.QueryDslConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static team9499.commitbody.domain.Member.domain.AccountStatus.*;
import static team9499.commitbody.domain.record.dto.response.RecordMonthResponse.*;

@Import(QueryDslConfig.class)
@DataJpaTest
class CustomRecordRepositoryImplTest {

    @Autowired private RecordRepository recordRepository;
    @Autowired private RecordDetailsRepository recordDetailsRepository;
    @Autowired private RecordSetsRepository recordSetsRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ExerciseRepository exerciseRepository;

    private Member member;
    private Record record;
    private Exercise exercise1;
    private Exercise exercise2;
    private Exercise exercise3;

    @BeforeEach
    void init(){

        member = memberRepository.save(Member.builder().nickname("닉네임").isWithdrawn(false).accountStatus(PUBLIC).BodyFatPercentage(20f).weight(78f).height(178f).build());
        exercise1 = exerciseRepository.save(new Exercise(1L,"등운동1","사진", ExerciseTarget.등, ExerciseType.WEIGHT_AND_REPS, ExerciseEquipment.CABLE,4f,new ArrayList<>()));
        exercise2 = exerciseRepository.save(new Exercise(2L,"등운동2","사진", ExerciseTarget.등, ExerciseType.REPS_ONLY, ExerciseEquipment.BAND,4f,new ArrayList<>()));
        exercise3 = exerciseRepository.save(new Exercise(3L,"등운동3","사진", ExerciseTarget.등, ExerciseType.TIME_ONLY, ExerciseEquipment.CARDIO,4f,new ArrayList<>()));

        record = recordRepository.save(Record.create("기록1", LocalDateTime.now(), LocalDateTime.of(2024,8,12,12,12).plusHours(2), 20, member));
        recordRepository.save(Record.create("기록1", LocalDateTime.now(), LocalDateTime.of(2024,7,12,12,12).plusHours(2), 2620, member));
        recordRepository.save(Record.create("기록1", LocalDateTime.now(), LocalDateTime.of(2024,8,16,12,12).plusHours(2), 20, member));

        RecordDetails details1 = recordDetailsRepository.save(RecordDetails.create(exercise1, record, 1));
        RecordDetails details2 = recordDetailsRepository.save(RecordDetails.create(exercise2, record, 2));
        RecordDetails details3 = recordDetailsRepository.save(RecordDetails.create(exercise3, record, 3));


        List<RecordSets> recordSets = new ArrayList<>();
        recordSets.add(RecordSets.ofWeightAndSets(15,10,details1));
        recordSets.add(RecordSets.ofWeightAndSets(20,10,details1));
        recordSets.add(RecordSets.ofWeightAndSets(25,10,details1));
        recordSets.add(RecordSets.ofSets(10,details2));
        recordSets.add(RecordSets.ofSets(20,details2));
        recordSets.add(RecordSets.ofTimes(2,details3));

        recordSetsRepository.saveAll(recordSets);
    }

    @DisplayName("저장된 기록을 조회합니다.")
    @Test
    void findByRecordId(){
        RecordResponse recordResponse = recordRepository.findByRecordId(record.getId(), member.getId());

        assertThat(recordResponse.getRecordName()).isEqualTo(record.getRecordName());
        assertThat(recordResponse.getDetails().size()).isEqualTo(3);
        assertThat(recordResponse.getDetails().get(0).getExerciseName()).isEqualTo("등운동1");
        assertThat(recordResponse.getDetails().get(1).getExerciseName()).isEqualTo("등운동2");
        assertThat(recordResponse.getDetails().get(2).getExerciseName()).isEqualTo("등운동3");
    }


    @DisplayName("기록 삭제")
    @Test
    void deleteRecord(){
        recordRepository.deleteRecord(record.getId(),member.getId());
        List<Record> all = recordRepository.findAll();
        assertThat(all.size()).isEqualTo(2);
    }


    @DisplayName("해당 달 기록 조회")
    @Test
    void getRecordCountAdnDataForMonth(){
        Map<String, RecordData> august = recordRepository.getRecordCountAdnDataForMonth(member.getId(), 2024, 8);
        Map<String, RecordData> july = recordRepository.getRecordCountAdnDataForMonth(member.getId(), 2024, 7);
        Map<String, RecordData> june = recordRepository.getRecordCountAdnDataForMonth(member.getId(), 2024, 6);
        
        assertThat(august).containsKey("12");
        assertThat(august).containsKey("16");
        assertThat(july).containsKey("12");
        assertThat(june).isEmpty();
    }
    
    @DisplayName("해당 달의 진행한 모든 기록 조회")
    @Test
    void getAllMonthRecords(){
        List<RecordDay> recordPage = recordRepository.getRecordPage(member.getId(), 2024, 8);
        assertThat(recordPage.size()).isEqualTo(2);
    }

}