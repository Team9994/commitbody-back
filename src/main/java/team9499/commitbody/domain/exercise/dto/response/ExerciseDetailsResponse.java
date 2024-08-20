package team9499.commitbody.domain.exercise.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.record.dto.response.RecordSetsResponse;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseDetailsResponse {

    private LocalDateTime date;
    private List<RecordSetsResponse> sets;

    @JsonIgnore
    private Long recordDetailId;
    public static ExerciseDetailsResponse of(LocalDateTime localDateTime,Long recordDetailId,List<RecordSetsResponse> test){
        return ExerciseDetailsResponse.builder().date(localDateTime).recordDetailId(recordDetailId).sets(test).build();
    }
}
