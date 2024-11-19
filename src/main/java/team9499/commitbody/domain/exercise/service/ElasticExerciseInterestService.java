package team9499.commitbody.domain.exercise.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;

import java.io.IOException;
import java.util.List;

public interface ElasticExerciseInterestService {

    SearchExerciseResponse searchFavoriteExercise(String memberId, BoolQuery.Builder builder,int size, int from,List<SortOptions> sortOptions);

    List<ExerciseDto> updateInterestFieldStatus(String memberId, List<ExerciseDto> response) throws IOException;

}
