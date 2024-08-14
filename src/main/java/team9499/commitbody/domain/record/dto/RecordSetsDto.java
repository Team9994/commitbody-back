package team9499.commitbody.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordSetsDto {

    private Integer weight;

    private Integer times;

    private Integer reps;
}
