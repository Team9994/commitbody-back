package team9499.commitbody.domain.routin.dto.rqeust;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoutineExercise {

    private Long exerciseId;
    private Integer order;
    private String source;
}
