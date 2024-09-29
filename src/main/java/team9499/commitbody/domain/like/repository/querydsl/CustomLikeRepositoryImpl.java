package team9499.commitbody.domain.like.repository.querydsl;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static team9499.commitbody.domain.article.domain.QArticle.*;
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

    /**
     * 사용자가 좋아요한 게시글의 ID를 조회
     * @param memberId  사용자 ID
     * @return  게시글 ID 리스트
     */
    @Override
    public List<Long> findArticleIdsByDeleteMember(Long memberId) {
        return jpaQueryFactory.select(article.id)
                .from(contentLike)
                .join(article).on(article.id.eq(contentLike.article.id)).fetchJoin()
                .where(contentLike.member.id.eq(memberId)
                        .and(article.member.id.ne(contentLike.member.id))
                        .and(contentLike.likeStatus.eq(true)))
                .fetch();
    }
}
