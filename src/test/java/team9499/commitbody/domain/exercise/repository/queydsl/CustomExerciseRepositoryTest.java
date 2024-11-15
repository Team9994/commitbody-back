package team9499.commitbody.domain.exercise.repository.queydsl;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import team9499.commitbody.global.config.QueryDslConfig;

@DataJpaTest
@Import(QueryDslConfig.class)
class CustomExerciseRepositoryTest {


}