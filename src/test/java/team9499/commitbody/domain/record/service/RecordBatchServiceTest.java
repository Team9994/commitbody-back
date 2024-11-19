package team9499.commitbody.domain.record.service;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import team9499.commitbody.domain.record.domain.RecordDetails;
import team9499.commitbody.domain.record.domain.RecordSets;


@ExtendWith(MockitoExtension.class)
class RecordBatchServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private RecordBatchService recordBatchService;

    @DisplayName("기록,기록세트 JDBC를 통한 삭제")
    @Test
    void deleteDetailsIdsInBatch() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        when(jdbcTemplate.batchUpdate(anyString(), anyList(), anyInt(), any())).thenReturn(new int[][]{});

        recordBatchService.deleteDetailsIdsInBatch(ids);

        verify(jdbcTemplate, times(1)).batchUpdate(eq("DELETE FROM record_details WHERE record_details_id = ?"),
                anyList(), anyInt(), any());
        verify(jdbcTemplate, times(1)).batchUpdate(eq("DELETE FROM record_sets WHERE record_details_id = ?"), anyList(),
                anyInt(), any());
    }

    @DisplayName("비동기를 통한 세트수 저장")
    @Test
    void insertSetsInBatch() {
        List<RecordSets> setsList = Arrays.asList(
                new RecordSets(10L, 50, 1, 10, new RecordDetails()),
                new RecordSets(8L, 40, 2, 10, new RecordDetails()),
                new RecordSets(12L, 60, 1, 10, new RecordDetails())
        );

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class))).thenReturn(new int[]{});

        recordBatchService.insertSetsInBatch(setsList);

        verify(jdbcTemplate, times(1)).batchUpdate(
                eq("INSERT INTO record_sets (reps, weight, times, record_details_id) VALUES (?, ?, ?, ?)"),
                any(BatchPreparedStatementSetter.class));
    }

}