package team9499.commitbody.domain.record.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.record.domain.RecordSets;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordSetsResponse {

    private Long setId;
    private Integer weight;
    private Integer reps;
    private Integer times;

    public static RecordSetsResponse of(Long id, Integer weight, Integer reps, Integer times){
        RecordSetsResponseBuilder builder = RecordSetsResponse.builder().setId(id);
        if (weight!=null && reps !=null){
            builder.weight(weight).reps(reps);
        }else if (times!=null){
            builder.times(times);
        }else
            builder.reps(reps);
        return builder.build();
    }

    public static RecordSetsResponse of(Integer weight, Integer reps, Integer times){
        return RecordSetsResponse.builder().weight(weight).reps(reps).times(times).build();
    }

    public static RecordSetsResponse of(RecordSets recordSets){
        return RecordSetsResponse.builder().weight(recordSets.getWeight())
                .reps(recordSets.getReps())
                .times(recordSets.getTimes()).build();
    }
}
