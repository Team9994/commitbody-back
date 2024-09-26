package team9499.commitbody.domain.routin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.routin.domain.RoutineDetails;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class RoutineBatchService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * jdbc를 사용한 배치 저장
     * @param routineDetails
     */
    public void saveRoutineDetailInBatch(List<RoutineDetails> routineDetails) {
        String sql = "INSERT INTO routine_details (exercise_id,total_sets, routine_id, orders) VALUES (?,?,?,?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RoutineDetails detail = routineDetails.get(i);
                ps.setLong(1, detail.getExercise().getId());
                ps.setInt(2,4);
                ps.setLong(3, detail.getRoutine().getId());
                ps.setInt(4, detail.getOrders());
            }

            @Override
            public int getBatchSize() {
                return routineDetails.size();
            }
        });
    }
}
