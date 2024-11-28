package team9499.commitbody.domain.record.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.record.domain.Record;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static team9499.commitbody.global.constants.Delimiter.*;
import static team9499.commitbody.global.utils.TimeConverter.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordMonthResponse {

    private Map<String,RecordData> dayRecordCount;        // 일별 진행 횟수

    private List<RecordDay> records;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecordData{
        private String day;
        private List<RecordDay> recordDays;

        public void setDays(Record record,List<RecordDay> recordDays) {
            this.day = converterTime(record);
            this.recordDays = recordDays;
        }
    }

    @Data
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecordDay {
        private Long recordId;
        private String recordName;  // 기록명
        private String durationTime;    // 진행시간
        private LocalDateTime lastTime; // 마지막 타임

        public RecordDay(Long recordId, String recordName, String durationTime) {
            this.recordId = recordId;
            this.recordName = recordName;
            this.durationTime = durationTime;
        }

        public static RecordDay of(Record record) {
            return RecordDay.builder().recordId(record.getId())
                    .recordName(record.getRecordName())
                    .durationTime(converterTime(record) + DOT_SEPARATOR + converterDurationTime(record)).build();

        }

        public static RecordDay recordPage_Of(Record record){
            return RecordDay.builder().recordId(record.getId())
                    .recordName(record.getRecordName())
                    .durationTime(converterTime(record) + DOT_SEPARATOR + converterDurationTime(record))
                    .lastTime(record.getEndTime())
                    .build();
        }
    }
}
