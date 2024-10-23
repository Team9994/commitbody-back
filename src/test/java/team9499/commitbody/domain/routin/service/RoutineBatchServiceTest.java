package team9499.commitbody.domain.routin.service;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.routin.domain.Routine;
import team9499.commitbody.domain.routin.domain.RoutineDetails;

@ExtendWith(MockitoExtension.class)
class RoutineBatchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RoutineBatchService routineService;


    private List<RoutineDetails> routineDetailsList;

    @BeforeEach
    void setUp() {
        Routine routine = new Routine();
        routine.setId(1L);

        Exercise exercise1 = new Exercise();
        exercise1.setId(101L);

        Exercise exercise2 = new Exercise();
        exercise2.setId(102L);

        RoutineDetails routineDetail1 = new RoutineDetails();
        routineDetail1.setRoutine(routine);
        routineDetail1.setExercise(exercise1);
        routineDetail1.setOrders(1);

        RoutineDetails routineDetail2 = new RoutineDetails();
        routineDetail2.setRoutine(routine);
        routineDetail2.setExercise(exercise2);
        routineDetail2.setOrders(2);

        routineDetailsList = new ArrayList<>();
        routineDetailsList.add(routineDetail1);
        routineDetailsList.add(routineDetail2);
    }

    @DisplayName("배치를통한 루틴 데이터 삽입")
    @Test
    void saveRoutineDetailInBatch() {

        when(jdbcTemplate.batchUpdate(anyString(),any(BatchPreparedStatementSetter.class))).thenReturn(new int[]{});

        routineService.saveRoutineDetailInBatch(routineDetailsList);

        verify(jdbcTemplate, times(1)).batchUpdate(eq("INSERT INTO routine_details (exercise_id,total_sets, routine_id, orders) VALUES (?,?,?,?)"), any(BatchPreparedStatementSetter.class));
    }
}