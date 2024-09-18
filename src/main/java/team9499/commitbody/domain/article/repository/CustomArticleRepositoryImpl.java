package team9499.commitbody.domain.article.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.article.domain.*;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.block.domain.QBlockMember;
import team9499.commitbody.domain.file.domain.File;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.like.domain.QContentLike;
import team9499.commitbody.global.notification.domain.QNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final QBlockMember fromBlock = new QBlockMember("fromBlock");

    /**
     * 게시글의 데이터를 카테고리별로 데이터를 무한스크롤 방식으로 조회합니다.
     * @param memberId  로그인한 사용자 ID
     * @param type  게시글 타입 [운동 인증 , 정보 & 질문]
     * @param articleCategory  검색 카테고리 [전체, 인기, 몸평, 피드백, 질문, 팔로잉]
     * @param lastId    마지막 게시글 ID
     * @param pageable  페이징 정보
     * @return SliceImpl
     */
    @Override
    public Slice<ArticleDto> getAllArticles(Long memberId, ArticleType type, ArticleCategory articleCategory, Long lastId, Pageable pageable) {
        BooleanBuilder booleanBuilder = getBooleanBuilder(lastId);
        BooleanBuilder categoryBuilder = new BooleanBuilder();
        BooleanBuilder followBuilder = new BooleanBuilder();
        // 정보&질문 게시글에서 인기글 탭을 클릭할시에 해당 달의 카테고리 별로 좋아요 순이 가장 많은 게시글 2개씩 조회하는 동적쿼리
        BooleanBuilder defaultBuilder = new BooleanBuilder();

        // 질문&정보 게시글일때 사용
        if (type.equals(ArticleType.INFO_QUESTION)) {
            if (!(articleCategory.equals(ArticleCategory.ALL) || articleCategory.equals(ArticleCategory.POPULAR)))  // 전체나 인기순이 아닐때만 카테고리 쿼리 적용
                categoryBuilder.and(article.articleCategory.eq(articleCategory));
        }

        // 인기글이 아니고 팔로잉 검색이 아닐때 팔로워된 사용자거나 공개범위 게시글을 보여주도록 하는 동적 쿼리
        if (!(categoryBuilder.equals(ArticleCategory.FOLLOWING) && articleCategory.equals(ArticleCategory.POPULAR))) {
            defaultBuilder.and(article.visibility.eq(Visibility.PUBLIC).or(article.member.id.eq(memberId)))
                    .or(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW)));
        }

        // 운동 인증 게시글에서의 팔로잉 탭을 클릭할시에만 적용하는 동적 쿼리
        if (type.equals(ArticleType.EXERCISE) && articleCategory.equals(ArticleCategory.FOLLOWING)) {
            followBuilder.and(follow.status.eq(FollowStatus.FOLLOWING).or(follow.status.eq(FollowStatus.MUTUAL_FOLLOW)))
                    .and(article.visibility.eq(Visibility.FOLLOWERS_ONLY).or(article.visibility.eq(Visibility.PUBLIC)));
        }

        //동적 쿼리 생성 카테고리가 '인기' 카테고리고 들어 올시에는 article 가 아닌 서브엔티티인 'articleRowSub'의 대해서 조회하도록 삼항 연산자 사용
        List<Tuple> tupleList = jpaQueryFactory.select(article, file, article.member)
                .from(article)
                .leftJoin(file).on(file.article.id.eq(article.id)).fetchJoin()
                .leftJoin(blockMember).on(blockMember.blocked.id.eq(memberId).and(blockMember.blocker.id.eq(article.member.id)))    // 차단한 사용자 필터링
                .leftJoin(fromBlock).on(fromBlock.blocked.id.eq(article.member.id).and(fromBlock.blocker.id.eq(memberId)))         // 차단된 사용자 필터링
                .leftJoin(follow).on(follow.follower.id.eq(memberId).and(follow.following.id.eq(article.member.id)))
                .where(booleanBuilder, categoryBuilder, followBuilder, defaultBuilder,
                        article.articleType.eq(type).and(blockMember.id.isNull().or(blockMember.blockStatus.eq(false)))
                                .and(fromBlock.id.isNull().or(fromBlock.blockStatus.eq(false))))
                .limit(pageable.getPageSize() + 1)
                .orderBy(article.createdAt.desc())
                .fetch();

        List<ArticleDto> articleDtoList = tupleList.stream().map(
                tuple -> ArticleDto.of(memberId,
                        tuple.get(article),
                        converterImgUrl(tuple.get(file)),
                        tuple.get(article.member),
                        null)
        ).collect(Collectors.toList());


        boolean hasNext = isHasNext(pageable, articleDtoList);

        return new SliceImpl<>(articleDtoList, pageable, hasNext);
    }

    /**
     * 프로필 접근시 운동 관련 게시글 조회
     *
     * @param loginMemberId 현재 로그인한 사용자 ID
     * @param findMemberId  조회할 사용자 ID
     * @param myAccount     자신 프로필인지 확인 [true : 자신 프로필, false : 상대프로필]
     * @param articleType   게시글 타입 두 종류
     * @param lastId        마지막 조회 게시글 ID
     * @param pageable      페이징 정보
     * @return Slice<ArticleDto> 객체 반환
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
        } else {  // 게시글 타입이 "정보&질문"일 경우
            collect = getInfoArticleQuery(loginMemberId, findMemberId, pageable, builder, validArticleFollow);
        }

        // 다음 페이지 존재 여부확인
        boolean hasNext = isHasNext(pageable, collect);

        return new SliceImpl(collect, pageable, hasNext);
    }

    /**
     * 게시글을 상세조회하는 쿼리
     *
     * @param loginMemberId 로그인한 사용자 ID
     * @param articleId     조회할 게시글 ID
     * @return ArticleDto 반환
     */
    @Override
    public ArticleDto getDetailArticle(Long loginMemberId, Long articleId) {

        List<Tuple> list = jpaQueryFactory.select(article, file, follow)
                .from(article)
                .leftJoin(file).on(article.id.eq(file.article.id)).fetchJoin()  // 파일 정보는 선택적이므로 LEFT JOIN
                .leftJoin(follow).on(follow.follower.id.eq(loginMemberId).and(follow.following.id.eq(article.member.id)))
                .where(article.id.eq(articleId)).fetch();

        return list.stream()
                .map(tuple -> ArticleDto.of(loginMemberId, tuple.get(article), converterImgUrl(tuple.get(file)), tuple.get(follow))).findFirst().get();
    }

    /**
     * 게시글과 게시글의 저장된 이미지 파일을 조회합니다.
     *
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


        return Map.of("article", tuple.get(article), "storedName", tuple.get(file.storedName) == null ? "" : tuple.get(file.storedName));
    }

    @Async
    @Transactional
    @Override
    public void deleteArticle(Long articleId) {
        jpaQueryFactory.delete(article).where(article.id.eq(articleId)).execute();
        jpaQueryFactory.delete(articleComment).where(articleComment.article.id.eq(articleId)).execute();
        jpaQueryFactory.delete(QNotification.notification).where(QNotification.notification.articleId.eq(articleId)).execute();
        jpaQueryFactory.delete(QContentLike.contentLike).where(QContentLike.contentLike.article.id.eq(articleId)).execute();
        jpaQueryFactory.delete(file).where(file.article.id.eq(articleId)).execute();
    }

    /*
    정보&질문 게시글일 경우 조회하는 쿼리
     */
    private List<ArticleDto> getInfoArticleQuery(Long loginMemberId, Long findMemberId, Pageable pageable, BooleanBuilder builder, BooleanBuilder validArticleFollow) {
        List<Tuple> articleList = jpaQueryFactory.select(article, file, articleComment.count())
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

        return articleList.stream()
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
        if (collect.size() > pageable.getPageSize()) {
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
        if (lastId != null) {
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
    private static BooleanBuilder validArticleFollow(boolean myAccount, Long loginMemberId) {
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
        return file == null ? "등록된 이미지가 없습니다." : cloudUrl + file.getStoredName();
    }
}