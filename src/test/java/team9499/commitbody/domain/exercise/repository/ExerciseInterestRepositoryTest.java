package team9499.commitbody.domain.exercise.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.ExerciseInterest;
import team9499.commitbody.global.config.QueryDslConfig;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Import(QueryDslConfig.class)
@ExtendWith(SpringExtension.class)
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles("test")
class ExerciseInterestRepositoryTest {


    @Autowired ExerciseRepository exerciseRepository;
    @Autowired ExerciseInterestRepository exerciseInterestRepository;
    @Autowired CustomExerciseRepository customExerciseRepository;
    @Autowired MemberRepository memberRepository;

//    @Test
//    void save(){
//        Member build = Member.builder().nickname("테스트").build();
//        memberRepository.save(build);
//
//        CustomExercise customExercise = CustomExercise.builder().customGifUrl("test").customExName("테스트 이름").member(build).build();
//        customExerciseRepository.save(customExercise);
//
//        ExerciseInterest build1 = ExerciseInterest.builder().isInterested(false).customExercise(customExercise).member(build).build();
//        ExerciseInterest save = exerciseInterestRepository.save(build1);
//
//        Boolean interRest = exerciseInterestRepository.getInterRest(null,customExercise.getId(), build1.getId());
//
//        log.info("save= {}", interRest);
//    }

}