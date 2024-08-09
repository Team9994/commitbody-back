package team9499.commitbody.domain.exercise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchExerciseResponse {

    private Long totalCount;

    private   List<LinkedHashMap<String,Object>> exercise;
}
