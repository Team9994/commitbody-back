package team9499.commitbody.domain.exercise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchExerciseResponse {

    private Long totalCount;

    private List<ExerciseDto> exercise;
}
