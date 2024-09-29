package team9499.commitbody.domain.like.repository.querydsl;

import java.util.List;

public interface CustomLikeRepository {

    void deleteByCustomExerciseId(Long customExerciseId);

    List<Long> findArticleIdsByDeleteMember(Long memberId);
}
