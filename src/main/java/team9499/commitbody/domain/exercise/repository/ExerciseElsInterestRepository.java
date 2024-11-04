package team9499.commitbody.domain.exercise.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.exercise.domain.ExerciseInterestDoc;

@Repository
public interface ExerciseElsInterestRepository extends ElasticsearchRepository<ExerciseInterestDoc, Long> {
}
