package team9499.commitbody.domain.comment.article.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;
import team9499.commitbody.domain.comment.article.dto.response.ArticleCommentResponse;
import team9499.commitbody.domain.comment.article.repository.ArticleCommentRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.notification.service.NotificationService;
import team9499.commitbody.global.redis.RedisService;

import java.util.List;

import static team9499.commitbody.global.constants.Delimiter.*;

@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ArticleCommentServiceImpl implements ArticleCommentService {

    private final static String COMMENT_SUCCESS = "댓글 작성 성공";
    private final static String REPLY_COMMENT_SUCCESS = "대댓글 작성 성공";

    private final ArticleCommentRepository articleCommentRepository;
    private final ArticleCommentBatchService articleCommentBatchService;
    private final ArticleRepository articleRepository;
    private final RedisService redisService;
    private final NotificationService notificationService;

    /**
     * 댓/대댓글 작성 메서드
     * parentId가 존재하지 않을시는 최상위 댓글이라 판단하여 댓글을 저장하며 parentId가 존재시 대댓글로 간주해여 parent 자식댓글 리스트의 대댓글을 저장합니다.
     *
     * @param memberId        작성자 ID
     * @param articleId       게시글 ID
     * @param commentParentId 부모댓글 ID
     * @param content         댓글 내용
     * @param replyNickname   멘션할 사용자 닉네임
     * @return ArticleCommentCountResponse 반환
     */
    @Override
    public ArticleCountResponse saveArticleComment(Long memberId, Long articleId, Long commentParentId,
                                                   String content, String replyNickname) {
        Article article = getArticle(articleId);
        Member member = getMember(memberId);
        if (commentParentId != null) {
            return handleChildComment(articleId, commentParentId, content, replyNickname, article, member);
        }
        return handleParentComment(articleId, content, article, member);

    }

    /**
     * 댓글 수정
     * 작성자만이 댓글을 수정 가능 비작성자 수정시 403 예외 발생
     *
     * @param memberId  로그인한 사용자 ID
     * @param commentId 변경할 댓글 ID
     * @param content   수정할 댓글 내용
     */
    @Override
    public void updateArticleComment(Long memberId, Long commentId, String content) {
        ArticleComment articleComment = getArticleComment(commentId);
        validWriter(memberId, articleComment);      // 상용자 검증
        articleComment.updateContent(content);      // 댓글 수정
        notificationService.updateNotification(commentId, content);      // 알림 내용 수정
    }


    /**
     * 게시글의 작성된 댓글 조회
     *
     * @param articleId     게시글 아이디
     * @param memberId      사용자 id
     * @param lastId        마지막 댓글 id
     * @param lastLikeCount 마지막 댓글 좋아요 수
     * @param orderType     정렬 타입
     * @param pageable      페이징 정보
     */
    @Transactional(readOnly = true)
    @Override
    public ArticleCommentResponse getComments(Long articleId, Long memberId, Long lastId, Integer lastLikeCount,
                                              OrderType orderType, Pageable pageable) {
        Slice<ArticleCommentDto> allCommentByArticle = articleCommentRepository
                .getAllCommentByArticle(articleId, memberId, lastId, lastLikeCount, orderType, pageable);
        Integer commentCount = articleCommentRepository.getCommentCount(articleId, memberId);
        return new ArticleCommentResponse(commentCount, allCommentByArticle.hasNext(), allCommentByArticle.getContent());
    }

    /**
     * 댓글의 작성된 대댓글 무한 스크롤 방식으로 조회
     *
     * @param commentId 조회할 댓글 ID
     * @param memberId  로그인한 사용자 ID
     * @param pageable  페이징 정보
     */
    @Transactional(readOnly = true)
    @Override
    public ArticleCommentResponse getReplyComments(Long commentId, Long memberId, Long lastId, Pageable pageable) {
        Slice<ArticleCommentDto> comments = articleCommentRepository
                .getAllReplyComments(commentId, memberId, lastId, pageable);
        return new ArticleCommentResponse(comments.hasNext(), comments.getContent());
    }

    /**
     * 게시글의 작성된 댓글을 삭제합니다.
     * 부모 댓글 삭제시 : 대댓글과, 좋아요, 댓글과 관련된 알림 기록등을 JDBC 배치를 사용해 삭제합니다.
     * 자식 댓글 삭제시 : 해당 대댓글과 좋아요 대댓글 관련 알림 기록등을 JDBC 배치를 사용해 삭제합니다.
     * 작성자가 아닐시 삭제 요청이오면 403 예외를 발생합니다.
     *
     * @param memberId  로그인한 사용자 ID
     * @param commentId 삭제할 댓글 ID
     * @return ArticleCommentCountResponse 반환
     */
    @Override
    public ArticleCountResponse deleteArticleComment(Long memberId, Long commentId) {
        ArticleComment articleComment = getArticleComment(commentId);
        validWriter(memberId, articleComment);
        if (validParentIsNull(articleComment)) {
            return handleDeleteParentComment(commentId, articleComment);
        }
        return handleReplyComment(commentId);
    }

    @Override
    public List<Long> getWriteDrawArticleIdsByComment(Long memberId) {
        return articleCommentRepository.findCommentArticleIdsByMemberId(memberId);
    }

    private ArticleCountResponse handleChildComment(Long articleId, Long commentParentId, String content,
                                                    String replyNickname, Article article, Member member) {
        ArticleComment parentComment = getParentComment(commentParentId);
        ArticleComment comment = saveArticleComment(ArticleComment.of(article, member, content, parentComment));
        parentComment.addChildComment(comment); // 부모 댓글에 대댓글 추가
        sendCommentNotification(articleId, replyNickname, content, member, article, parentComment, comment);
        return ArticleCountResponse.of(articleId, null, REPLY_COMMENT_SUCCESS);
    }

    private ArticleComment getParentComment(Long commentParentId) {
        return articleCommentRepository.getReferenceById(commentParentId);
    }

    private ArticleCountResponse handleParentComment(Long articleId, String content, Article article, Member member) {
        ArticleComment parentComment = ArticleComment.of(article, member, content, null);// 부모 댓글 없이 최상위 댓글 생성
        sendCommentNotification(articleId, STRING_EMPTY, content, member, article, null, saveArticleComment(parentComment));    // 댓글 알림 전송
        int commentCount = article.getCommentCount() + 1;
        article.updateCommentCount(commentCount);
        return ArticleCountResponse.of(articleId, commentCount, COMMENT_SUCCESS);
    }


    private ArticleComment saveArticleComment(ArticleComment parentComment) {
        return articleCommentRepository.save(parentComment);
    }

    private void sendCommentNotification(Long articleId, String replyNickname, String content, Member member,
                                         Article article, ArticleComment parentComment, ArticleComment articleComment) {
        String commentId = articleComment.getId().toString();
        String title = article.getTitle();
        if (!replyNickname.isEmpty()) {
            notificationService.sendReplyComment(member, replyNickname, title, parentComment.getContent(), commentId, articleId);
            return;
        }
        notificationService.sendComment(member, article.getMember().getId(), title, content, commentId, articleId);
    }

    private Article getArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    private Member getMember(Long memberId) {
        return redisService.getMemberDto(String.valueOf(memberId))
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
    }

    private ArticleComment getArticleComment(Long commentId) {
        return articleCommentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    private static boolean validParentIsNull(ArticleComment articleComment) {
        return articleComment.getParent() == null;
    }

    private ArticleCountResponse handleDeleteParentComment(Long commentId, ArticleComment articleComment) {
        Article article = getArticle(articleComment.getArticle().getId());
        int count = article.getCommentCount() - 1;
        article.updateCommentCount(count);  // 부모 댓글 삭제시에는 게시글의 작성된 댓글수 -1
        List<Long> deleteIds = articleCommentRepository.getAllChildComment(commentId);  // 작성된 댓글의 대댓글의 ID를 리스트화
        articleCommentBatchService.deleteCommentBatch(commentId, deleteIds); // 배치를 통해 댓글과 관련된 모든 데이터 삭제
        return ArticleCountResponse.of(article.getId(), count, null);
    }

    private ArticleCountResponse handleReplyComment(Long commentId) {
        articleCommentBatchService.deleteChildCommentBatch(commentId);
        return null;
    }

    private static void validWriter(Long memberId, ArticleComment articleComment) {
        if (!articleComment.getMember().getId().equals(memberId)) {
            throw new InvalidUsageException(ExceptionStatus.FORBIDDEN, ExceptionType.AUTHOR_ONLY);
        }
    }

}
