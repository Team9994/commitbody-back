package team9499.commitbody.domain.like.exercise.repository.querydsl;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static team9499.commitbody.domain.comment.exercise.domain.QExerciseComment.*;
import static team9499.commitbody.domain.like.exercise.domain.QExerciseCommentLike.*;

@Repository
@RequiredArgsConstructor
public class CustomExerciseCommentLikeRepositoryImpl implements CustomExerciseCommentLikeRepository{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public void deleteByCustomExerciseId(Long customExerciseId) {
        jpaQueryFactory.delete(exerciseCommentLike)
                .where(exerciseComment.id.in(
                        JPAExpressions.select(exerciseComment.id)
                                .from(exerciseComment)
                                .where(exerciseComment.customExercise.id.eq(customExerciseId))
                )).execute();
    }
}
