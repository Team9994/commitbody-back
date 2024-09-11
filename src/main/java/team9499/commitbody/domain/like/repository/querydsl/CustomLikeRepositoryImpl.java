package team9499.commitbody.domain.like.repository.querydsl;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static team9499.commitbody.domain.comment.exercise.domain.QExerciseComment.*;
import static team9499.commitbody.domain.like.domain.QContentLike.*;

@Repository
@RequiredArgsConstructor
public class CustomLikeRepositoryImpl implements CustomLikeRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public void deleteByCustomExerciseId(Long customExerciseId) {
        jpaQueryFactory.delete(contentLike)
                .where(exerciseComment.id.in(
                        JPAExpressions.select(exerciseComment.id)
                                .from(exerciseComment)
                                .where(exerciseComment.customExercise.id.eq(customExerciseId))
                )).execute();
    }
}
