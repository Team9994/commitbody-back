package team9499.commitbody.domain.exercise.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import team9499.commitbody.domain.exercise.domain.ExerciseDoc;

public interface ExerciseElsRepository extends ElasticsearchRepository<ExerciseDoc,String> {
}
