package team9499.commitbody.domain.record.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.record.domain.RecordSets;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class RecordBatchService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 비동기를 통한 기록의 상세 종목과 관련된 세트수 대량 삭제
     * 오류가 발생하더라고 다음 반 요청에서 삭제 할 수있다.
     */
    @Async
    public void deleteDetailsIdsInBatch(List<Long> ids) {
        String detailsSql = "DELETE FROM record_details WHERE record_details_id = ?";
        String setSql = "DELETE FROM record_sets WHERE record_details_id = ?";

        jdbcTemplate.batchUpdate(detailsSql, ids, ids.size(),
                (ps, id) -> ps.setLong(1, id)
        );

        jdbcTemplate.batchUpdate(setSql,ids, ids.size(),
                (ps,id) -> ps.setLong(1,id)
                );
    }

    /**
     * 비동기를 통한 세트수 저장
     * 한번이라도 오류가 발생한다면 비동기기 제거 예정
     */
    @Async
    public void insertSetsInBatch(List<RecordSets> setsList) {
        String sql = "INSERT INTO record_sets (reps, weight, times, record_details_id) VALUES (?, ?, ?, ?)";

        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    RecordSets recordSets = setsList.get(i);

                    // Set the parameters based on the fields in recordSets
                    if (recordSets.getReps() != null) {
                        ps.setInt(1, recordSets.getReps());
                    } else {
                        ps.setNull(1, java.sql.Types.INTEGER);
                    }

                    if (recordSets.getWeight() != null) {
                        ps.setInt(2, recordSets.getWeight());
                    } else {
                        ps.setNull(2, java.sql.Types.INTEGER);
                    }

                    if (recordSets.getTimes() != null) {
                        ps.setInt(3, recordSets.getTimes());
                    } else {
                        ps.setNull(3, java.sql.Types.INTEGER);
                    }

                    ps.setLong(4, recordSets.getRecordDetails().getId());
                }

                @Override
                public int getBatchSize() {
                    return setsList.size();
                }
            });
        }catch (Exception e){
            log.error("기록 세트 배치 업데이트중 오류 발생");
        }
    }
}
