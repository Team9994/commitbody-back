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
import team9499.commitbody.domain.Member.domain.QMember;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.block.domain.BlockMember;
import team9499.commitbody.domain.block.domain.QBlockMember;
import team9499.commitbody.domain.file.domain.File;
import team9499.commitbody.domain.follow.domain.Follow;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.follow.domain.QFollow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.Member.domain.QMember.*;
import static team9499.commitbody.domain.article.domain.QArticle.*;
import static team9499.commitbody.domain.block.domain.QBlockMember.*;
import static team9499.commitbody.domain.comment.article.domain.QArticleComment.*;
import static team9499.commitbody.domain.file.domain.QFile.*;
import static team9499.commitbody.domain.follow.domain.QFollow.*;

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
     * @param myAccount 자신 프로필인지 확인 [true : 자신 프로필, false : 상대프로필]
     * @param articleType   게시글 타입 두 종류
     * @param lastId    마지막 조회 게시글 ID
     * @param pageable  페이징 정보
     * @return  Slice<ArticleDto> 객체 반환
     */
    @Override
    public Slice<ArticleDto> getAllProfileArticle(Long loginMemberId, Long findMemberId, boolean myAccount, ArticleType articleType, Long lastId, Pageable pageable) {
        // 다음 페이지 동적 쿼리
        BooleanBuilder builder = getBooleanBuilder(lastId);
        // 게시글 공개 상태의 따른 동적 쿼리
        BooleanBuilder validArticleFollow = validArticleFollow(myAccount, loginMemberId);

        List<ArticleDto> collect = new ArrayList<>();
        // 게시글 타입이 "운동 인증" 일경우
        if (articleType.equals(ArticleType.EXERCISE)) {
            collect = getExerciseArticleQuery(loginMemberId, findMemberId, pageable, builder, validArticleFollow);
        }else{  // 게시글 타입이 "정보&질문"일 경우
            collect = getInfoArticleQuery(loginMemberId, findMemberId, pageable, builder, validArticleFollow);
        }

        // 다음 페이지 존재 여부확인
        boolean hasNext = isHasNext(pageable, collect);

        return new SliceImpl(collect,pageable,hasNext);
    }

    /**
     * 게시글을 상세조회하는 쿼리
     * @param loginMemberId 로그인한 사용자 ID
     * @param articleId 조회할 게시글 ID
     * @return ArticleDto 반환
     */
    @Override
    public ArticleDto getDetailArticle(Long loginMemberId, Long articleId) {

        List<Tuple> list = jpaQueryFactory.select(article, file,follow)
                .from(article)
                .leftJoin(file).on(article.id.eq(file.article.id)).fetchJoin()  // 파일 정보는 선택적이므로 LEFT JOIN
                .leftJoin(follow).on(follow.follower.id.eq(loginMemberId).and(follow.following.id.eq(article.member.id)))
                .where(article.id.eq(articleId)).fetch();

        return list.stream()
                .map(tuple -> ArticleDto.of(loginMemberId,tuple.get(article), converterImgUrl(tuple.get(file)),tuple.get(follow))).findFirst().get();
    }

    /**
     * 게시글과 게시글의 저장된 이미지 파일을 조회합니다.
     * @param articleId
     * @return
     */
    @Override
    public Map<String, Object> getArticleAndFile(Long articleId) {
        Tuple tuple = jpaQueryFactory.select(article, file.storedName)
                .from(article)
                .leftJoin(file).on(file.article.id.eq(article.id)).fetchJoin()
                .where(article.id.eq(articleId))
                .fetchOne();


        return Map.of("article",tuple.get(article),"storedName",tuple.get(file.storedName) == null ? "" : tuple.get(file.storedName));
    }

    /*
    정보&질문 게시글일 경우 조회하는 쿼리
     */
    private List<ArticleDto> getInfoArticleQuery(Long loginMemberId, Long findMemberId, Pageable pageable, BooleanBuilder builder, BooleanBuilder validArticleFollow) {
        List<Tuple> articleList =jpaQueryFactory.select(article, file, articleComment.count())
                .from(article)
                .leftJoin(file).on(article.id.eq(file.article.id)).fetchJoin()  // 파일 정보는 선택적이므로 LEFT JOIN
                .leftJoin(articleComment).on(articleComment.article.id.eq(article.id)).fetchJoin()  // 댓글 정보는 선택적이므로 LEFT JOIN
                .leftJoin(follow).on(article.member.id.eq(follow.follower.id).and(follow.following.id.eq(loginMemberId)))  // 팔로우 정보
                .where(builder, validArticleFollow, article.member.id.eq(findMemberId)
                        .and(article.articleType.eq(ArticleType.INFO_QUESTION)))
                .limit(pageable.getPageSize() + 1)  // 페이지 크기 + 1
                .groupBy(article.id, file.id, follow.id)  // 중복 제거를 위한 GROUP BY
                .orderBy(article.createdAt.desc())  // 최신 순으로 정렬
                .fetch();

        return  articleList.stream()
                 .map(tuple -> ArticleDto.of(tuple.get(article), converterImgUrl(tuple.get(file))))
                 .collect(Collectors.toList());
    }

    private List<ArticleDto> getExerciseArticleQuery(Long loginMemberId, Long findMemberId, Pageable pageable, BooleanBuilder builder, BooleanBuilder validArticleFollow) {
        List<Tuple> articleList = jpaQueryFactory.select(article, file)
                .from(article)
                .join(file).on(article.id.eq(file.article.id)).fetchJoin()  // 파일 정보는 선택적이므로 LEFT JOIN
                .leftJoin(follow).on(article.member.id.eq(follow.follower.id).and(follow.following.id.eq(loginMemberId)))  // 팔로우 정보
                .where(builder, validArticleFollow, article.member.id.eq(findMemberId).and(article.articleType.eq(ArticleType.EXERCISE)))
                .limit(pageable.getPageSize() + 1)  // 페이지 크기 + 1
                .orderBy(article.createdAt.desc())  // 최신 순으로 정렬
                .fetch();

        return articleList.stream().map(tuple -> ArticleDto.of(tuple.get(article).getId(), converterImgUrl(tuple.get(file))))
                .collect(Collectors.toList());
    }

    /*
    조회된 리스트에서 조회한 사이즈보다 많은지 확인 메서드
     */
    private static boolean isHasNext(Pageable pageable, List<ArticleDto> collect) {
        boolean hasNext = false;
        if (collect.size() > pageable.getPageSize()){
            hasNext = true;
            collect.remove(pageable.getPageSize());
        }
        return hasNext;
    }

    /*
    다음 페이지 존재시 조회된 게시글보다 낮은 게시글 ID 부터 조회하는 동적 쿼리
     */
    private static BooleanBuilder getBooleanBuilder(Long lastId) {
        BooleanBuilder builder = new BooleanBuilder();
        if (lastId !=null){
            builder.and(article.id.lt(lastId));
        }
        return builder;
    }

    /*
    게시글의 상태의따라 필터링 해주는 동적 쿼리
    1. PUBLIC 일때 모든 사용자에게 조회 가능
    2. FOLLOWERS_ONLY 일때 팔로잉한 사용자에게만 조회 가능
    3. PRIVATE 일때 작성자만 조회 가능
     */
    private static BooleanBuilder validArticleFollow(boolean myAccount,Long loginMemberId){
        BooleanBuilder builder = new BooleanBuilder();
        if (!myAccount) {
            builder.and(article.visibility.eq(Visibility.PUBLIC)
                    .or(article.visibility.eq(Visibility.FOLLOWERS_ONLY)
                            .and(follow.status.eq(FollowStatus.FOLLOWING)
                                    .or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW))))
                    .or(article.visibility.eq(Visibility.PRIVATE)
                            .and(article.member.id.eq(loginMemberId))));
        }
        return builder;
    }

    /*
    등록된 이미지를 저장된 유혀한 값을 s3에 저장된 url로 변환 해주는 메서드
     */
    private String converterImgUrl(File file) {
        return file==null ? "등록된 이미지가 없습니다." : cloudUrl+file.getStoredName();
    }
}
