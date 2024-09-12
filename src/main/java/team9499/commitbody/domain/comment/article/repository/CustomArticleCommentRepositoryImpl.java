package team9499.commitbody.domain.comment.article.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.block.domain.QBlockMember;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.domain.QArticleComment;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;
import team9499.commitbody.global.utils.TimeConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.Member.domain.QMember.*;
import static team9499.commitbody.domain.block.domain.QBlockMember.blockMember;
import static team9499.commitbody.domain.comment.article.domain.QArticleComment.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomArticleCommentRepositoryImpl implements CustomArticleCommentRepository {

    private final JPAQueryFactory jpaQueryFactory;

    private final QArticleComment childComment = new QArticleComment("childComment");       // 자식 댓글
    private final QBlockMember childBlock = new QBlockMember("childBlock");       // 자식 댓글

    @Override
    public Slice<ArticleCommentDto> getAllCommentByArticle(Long articleId, Long memberId, Long lastId, Integer lastLikeCount,OrderType orderType, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // lastId를 기준으로 이전 댓글만 가져옴
        if (orderType.equals(OrderType.LIKE) && lastLikeCount!=null){
            if (lastLikeCount!=0){
                builder.or(articleComment.likeCount.lt(lastLikeCount));
            }else
                builder.or(articleComment.likeCount.eq(0).and(articleComment.id.lt(lastId)));
        }else if (lastId!=null){        // 최신순 정렬일 경우에는 lastId 값 기준으로 다음 데이터 조회
            builder.and(articleComment.id.lt(lastId));
        }

        OrderSpecifier[] order = getSortOrder(orderType,articleComment);
        // 부모 댓글만 가져오기 위한 조건 추가
        List<Tuple> comments = jpaQueryFactory
                .select(articleComment, articleComment.member)
                .from(articleComment)
                .leftJoin(articleComment.member, member).fetchJoin()
                .leftJoin(blockMember).on(blockMember.blocked.id.eq(articleComment.member.id).and(blockMember.blocker.id.eq(memberId)))
                .leftJoin(childBlock).on(childBlock.blocked.id.eq(memberId).and(childBlock.blocker.id.eq(articleComment.member.id)))    // 차단된 사용자 필터링 조건
                .where(articleComment.article.id.eq(articleId)
                        .and(articleComment.parent.id.isNull())
                        .and(blockMember.id.isNull().or(blockMember.blockStatus.eq(false)))
                        .and(childBlock.id.isNull().or(childBlock.blockStatus.eq(false)))
                ) // 차단되지 않았거나 차단이 해제된 경우
                .orderBy(order) // 최신 댓글 우선 정렬
                .limit(pageable.getPageSize() + 1)
                .fetch();

        // 조회된 댓글을 순회하며 articleComments 매핑하여 객체를 담습니다.
        List<ArticleCommentDto> articleComments = comments.stream()
                .map(tuple -> ArticleCommentDto.of(
                        tuple.get(articleComment),
                        MemberDto.toMemberDTO(tuple.get(articleComment.member)),
                        TimeConverter.converter(tuple.get(articleComment).getCreatedAt()),
                        checkWriter(tuple.get(articleComment.member), memberId),
                        true
                        )
                ).collect(Collectors.toList());

        boolean hasNext = articleComments.size() > pageable.getPageSize();   // 페이지 체크

        // 다음 페이지 여부를 결정하기 위해 마지막 요소를 제거
        if (hasNext) {
            articleComments.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(articleComments, pageable, hasNext);
    }

    /**
     * 게시글의 작성된 댓글의 총 값을 계산 해주는 쿼리
     * @param articleId 조회할 게시글 ID
     * @param memberId  로그인한 사용자 ID
     * @return  계한산 개시글 댓글 수
     */
    @Override
    public Integer getCommentCount(Long articleId, Long memberId) {
        List<ArticleComment> fetch = jpaQueryFactory
                .select(articleComment)
                .from(articleComment)
                .leftJoin(blockMember).on(blockMember.blocked.id.eq(articleComment.member.id).and(blockMember.blocker.id.eq(memberId)))
                .leftJoin(childBlock).on(childBlock.blocked.id.eq(memberId).and(childBlock.blocker.id.eq(articleComment.member.id)))    // 차단된 사용자 필터링 조건
                .where(articleComment.article.id.eq(articleId)
                        .and(articleComment.parent.id.isNull())
                        .and(blockMember.id.isNull().or(blockMember.blockStatus.eq(false)))
                        .and(childBlock.id.isNull().or(childBlock.blockStatus.eq(false)))
                ).fetch();
        return fetch.size();
    }

    /**
     * 댓글의 작성된 대댓글을 조회합니다.
     * 조회하는 사용자가 차단한 사용가 있을경우 차단한 사용자의 댓글을 필터링 한 후 조회
     * 조회하는 사용자를 차단한 사용자가 있을경우 차단한 사용자의 댓글을 필터링 한 후 조회
     * @param commentId 조회할 부모 댓글 ID
     * @param memberId  로그인한 사용자 ID
     * @param pageable  페이징 정보
     */
    @Override
    public Slice<ArticleCommentDto> getAllReplyComments(Long commentId, Long memberId, Pageable pageable) {

        List<Tuple> articleComments = jpaQueryFactory
                .select(articleComment, articleComment.member)
                .from(articleComment)
                .leftJoin(articleComment.childComments, childComment).fetchJoin() // 자식 댓글을 페치 조인
                .leftJoin(childComment.member, member).fetchJoin() // 자식 댓글 작성자 조인
                .leftJoin(blockMember).on(blockMember.blocked.id.eq(articleComment.member.id).and(blockMember.blocker.id.eq(memberId))) // 차단한 사용자 필터링 조건
                .leftJoin(childBlock).on(childBlock.blocked.id.eq(memberId).and(childBlock.blocker.id.eq(articleComment.member.id)))    // 차단된 사용자 필터링 조건
                .where(articleComment.parent.id.eq(commentId) // 부모 댓글 필터링
                        .and(blockMember.id.isNull().or(blockMember.blockStatus.eq(false)))
                        .and(childBlock.id.isNull().or(childBlock.blockStatus.eq(false)))
                ) // 차단된 사용자가 아니거나 차단이 해제된 경우만
                .limit(pageable.getPageSize() + 1)
                .orderBy(articleComment.createdAt.desc())
                .fetch();

        List<ArticleCommentDto> content = articleComments.stream()
                .map(tuple -> ArticleCommentDto.of(
                        tuple.get(articleComment),
                        TimeConverter.converter(tuple.get(articleComment).getCreatedAt()),
                        checkWriter(tuple.get(articleComment.member), memberId),
                        false
                )).collect(Collectors.toList());

        boolean hasNext =false;
        if (content.size() > pageable.getPageSize()){
            hasNext = true;
            content.remove(pageable.getPageSize());
        }

       return new SliceImpl<>(content,pageable,hasNext);
    }

    /**
     * 동적 정렬
     * - RECENT 최신순으로 정렬합니다.
     * - LIKE 좋아요 순으로 정렬합니다. 먼저 좋여가 많은순으로 정렬후 그이후 좋아요가 모두 0이라면 ID를 기준으로 내림차순 정렬 합니다.
     */
    private OrderSpecifier[] getSortOrder(OrderType orderType, QArticleComment articleComment) {
        List<OrderSpecifier> orderSpecifiers = new ArrayList<>();


        if (orderType.equals(OrderType.RECENT)){      // 최신순졍렬
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC,articleComment.id));
        }else if (orderType.equals(OrderType.LIKE)){    // 좋아요순 정렬
            // 좋아요 크기 순으로 먼저 내림차순으로 정렬합니다.
            OrderSpecifier<Integer> orderByLike = new CaseBuilder()
                    .when(articleComment.likeCount.gt(0)).then(articleComment.likeCount)
                    .otherwise(Expressions.constant(0)).desc();

            // 좋아요 크기가 0이라면 ID기준으로 내림차순으로 정렬합니다.
            OrderSpecifier<Long> orderById = new CaseBuilder()
                    .when(articleComment.likeCount.eq(0)).then(articleComment.id)
                    .otherwise(Expressions.constant(Long.MAX_VALUE)).desc();
            orderSpecifiers.add(orderByLike);
            orderSpecifiers.add(orderById);
        }
        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

    /*
    댓글 작성자일경우 true 작성자가 아닐경우 false 반환
     */
    static boolean checkWriter(Member member, Long memberId){
        return member.getId().equals(memberId);
    }
}
