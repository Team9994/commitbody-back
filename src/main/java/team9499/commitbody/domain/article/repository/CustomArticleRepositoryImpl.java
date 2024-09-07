package team9499.commitbody.domain.article.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.file.domain.File;

import java.util.List;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.article.domain.QArticle.*;
import static team9499.commitbody.domain.file.domain.QFile.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomArticleRepositoryImpl implements CustomArticleRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Value("${cloud.aws.cdn.url}")
    private String cloudUrl;

    /**
     * 프로필 접근시 운동 관련 게시글 조회
     * @param loginMemberId 현재 로그인한 사용자 ID
     * @param findMemberId  조회할 사용자 ID
     * @param lastId    조회된 마지막 게시글 ID
     * @param pageable
     * @return
     */
    @Override
    public Slice<ArticleDto> getAllExerciseArticle(Long loginMemberId, Long findMemberId, Long lastId, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        if (lastId!=null){
            builder.and(article.id.lt(lastId));
        }
        // 조회할 사용자가 로그인한 사용자일경우와 아닐경우의 따른 memberId
        Long memberId = loginMemberId == findMemberId ? loginMemberId: findMemberId;

        List<Tuple> articleList = jpaQueryFactory.select(article, file)
                .from(article)
                .join(file).on(article.id.eq(file.article.id)).fetchJoin()
                .where(builder, article.member.id.eq(memberId).and(article.articleType.eq(ArticleType.EXERCISE)))
                .limit(pageable.getPageSize() + 1)
                .orderBy(article.createdAt.desc())
                .fetch();

        List<ArticleDto> collect = articleList.stream().map(tuple -> ArticleDto.of(tuple.get(article).getId(), converterImgUrl(tuple.get(file))))
                .collect(Collectors.toList());

        boolean hasNext = false;
        if (collect.size() > pageable.getPageSize()){
            hasNext = true;
            collect.remove(pageable.getPageSize());
        }

        return new SliceImpl(collect,pageable,hasNext);
    }

    private String converterImgUrl(File file) {
        String storedName = file.getStoredName();
        return cloudUrl+storedName;
    }
}
