package team9499.commitbody.domain.exercise.dto;

import lombok.*;
import team9499.commitbody.domain.record.domain.Record;

import java.time.DayOfWeek;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class WeekReport {

    private DayOfWeek dayOfWeek;
    private int data;

    public static WeekReport of(Record record){

        return new WeekReport(record.getStartTime().getDayOfWeek(), record.getRecordVolume());
    }
}
