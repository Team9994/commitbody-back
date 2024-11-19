package team9499.commitbody.domain.exercise.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.ExerciseInterest;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.global.config.QueryDslConfig;

@DataJpaTest
@Import(QueryDslConfig.class)
class ExerciseInterestRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private CustomExerciseRepository customExerciseRepository;
    @Autowired
    private ExerciseInterestRepository exerciseInterestRepository;


    private Member member;
    private Exercise exercise;
    private CustomExercise customExercise;

    @BeforeEach
    void init() {
        member = memberRepository.save(Member.createSocialId("testId", LoginType.KAKAO, "default.png"));
        exercise = exerciseRepository.save(
                new Exercise(1L, "기본 운동", "exercise.com", ExerciseTarget.등, ExerciseType.TIME_ONLY,
                        ExerciseEquipment.BAND, 1.1f, new ArrayList<>()));
        customExercise = customExerciseRepository.save(
                CustomExercise.builder().customExName("커스텀 운동").exerciseEquipment(ExerciseEquipment.BAND)
                        .exerciseTarget(ExerciseTarget.등).member(member).build());
    }

    @DisplayName("기본 운동과 사용자의 Id를 통한 관심운동 조회")
    @Test
    void findByExerciseIdAndMemberId() {
        Optional<ExerciseInterest> interestEmpty = exerciseInterestRepository.findByExerciseIdAndMemberId(
                exercise.getId(), member.getId());

        exerciseInterestRepository.save(ExerciseInterest.exerciseInterest(member,exercise));
        Optional<ExerciseInterest> interestNotEmpty = exerciseInterestRepository.findByExerciseIdAndMemberId(
                exercise.getId(), member.getId());

        assertThat(interestEmpty).isEmpty();
        assertThat(interestNotEmpty).isNotEmpty();
    }

    @DisplayName("커스텀 운동과 사용자의 Id를 통한 관심운동 조회")
    @Test
    void findByCustomExerciseIdAndMemberId() {
        Optional<ExerciseInterest> interestEmpty = exerciseInterestRepository.findByCustomExerciseIdAndMemberId(
                customExercise.getId(), member.getId());

        exerciseInterestRepository.save(ExerciseInterest.exerciseInterest(member,customExercise));
        Optional<ExerciseInterest> interestNotEmpty = exerciseInterestRepository.findByCustomExerciseIdAndMemberId(
                customExercise.getId(), member.getId());

        assertThat(interestEmpty).isEmpty();
        assertThat(interestNotEmpty).isNotEmpty();
    }

    @DisplayName("커스텀 운동 Id를 통한 전체 삭제")
    @Test
    void deleteAllByCustomExerciseId(){
        exerciseInterestRepository.save(ExerciseInterest.exerciseInterest(member,customExercise));
        exerciseInterestRepository.save(ExerciseInterest.exerciseInterest(member,customExercise));
        exerciseInterestRepository.save(ExerciseInterest.exerciseInterest(member,customExercise));

        exerciseInterestRepository.deleteAllByCustomExerciseId(customExercise.getId());
        List<ExerciseInterest> all = exerciseInterestRepository.findAll();
        assertThat(all).isEmpty();

    }
}