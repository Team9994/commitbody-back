package team9499.commitbody.domain.exercise.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import team9499.commitbody.domain.exercise.domain.ExerciseInterestDoc;

public interface ExerciseElsInterestRepository extends ElasticsearchRepository<ExerciseInterestDoc, Long> {
}
