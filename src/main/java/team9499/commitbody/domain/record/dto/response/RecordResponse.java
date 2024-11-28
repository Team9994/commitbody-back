package team9499.commitbody.domain.record.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.record.domain.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static team9499.commitbody.global.utils.TimeConverter.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordResponse {

    private Long recordId;
    private String recordName;
    private String startDate;       // 운동 시작 날짜 및 시간
    private String durationTime;    // 운동 시간 (예: "60분")
    private Integer duration;       // 운동 시간 (분 단위, 예: 60)
    private Integer recordVolume;   // 볼륨 (kg 단위, 예: 100)
    private Integer recordSets;     // 세트 수 (예: 40)
    private Integer recordCalorie;  // 소모 칼로리 (kcal 단위, 예: 200)
    private List<RecordDetailsResponse> details;


    public static RecordResponse of(Record record, Map<Long, RecordDetailsResponse> detailsMap){
        return new RecordResponse(
                record.getId(),
                record.getRecordName(),
                converterTime(record),
                converterDurationTime(record),
                record.getDuration(),
                record.getRecordVolume(),
                record.getRecordSets(),
                record.getRecordCalorie(),
                new ArrayList<>(detailsMap.values())
        );
    }

}
