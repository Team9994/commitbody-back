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

@Service
@Transactional
@RequiredArgsConstructor
public class ArticleCommentServiceImpl implements ArticleCommentService{

    private final ArticleCommentRepository articleCommentRepository;
    private final ArticleCommentBatchService articleCommentBatchService;
    private final ArticleRepository articleRepository;
    private final RedisService redisService;
    private final NotificationService notificationService;

    /**
     * 댓/대댓글 작성 메서드
     * parentId가 존재하지 않을시는 최상위 댓글이라 판단하여 댓글을 저장하며 parentId가 존재시 대댓글로 간주해여 parent 자식댓글 리스트의 대댓글을 저장합니다.
     * @param memberId  작성자 ID
     * @param articleId 게시글 ID
     * @param commentParentId   부모댓글 ID
     * @param content   댓글 내용
     * @param replyNickname 멘션할 사용자 닉네임
     * @return ArticleCommentCountResponse 반환
     */
    @Override
    public ArticleCountResponse saveArticleComment(Long memberId, Long articleId, Long commentParentId, String content, String replyNickname) {
        // 게시글 조회
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));

        // 댓글 작성자 정보 조회
        Member member = redisService.getMemberDto(String.valueOf(memberId))
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));

        ArticleComment articleComment;
        String commentType ="";
        Integer commentCount = null;

        // 부모 댓글이 존재하는 경우 대댓글로 처리
        if (commentParentId != null) {
            ArticleComment parentComment = articleCommentRepository.getReferenceById(commentParentId);
            articleComment = ArticleComment.of(article, member, content, parentComment); // 부모 댓글을 포함해 대댓글 생성

            ArticleComment save = articleCommentRepository.save(articleComment);// 댓글 저장
            parentComment.addChildComment(articleComment); // 부모 댓글에 대댓글 추가
            commentType = "대댓글 작성 성공";
            // 비동기를 통한 알림 전송
            if (replyNickname!=null)
                notificationService.sendReplyComment(member,replyNickname, article.getTitle(),parentComment.getContent(),String.valueOf(save.getId()),articleId);
        } else { // 부모 댓글인 경우
            articleComment = ArticleComment.of(article, member, content, null); // 부모 댓글 없이 최상위 댓글 생성
            ArticleComment save = articleCommentRepository.save(articleComment);// 댓글 저장
            commentType ="댓글 작성 성공";
            notificationService.sendComment(member,article.getMember().getId(),article.getTitle(),content,String.valueOf(save.getId()),articleId);    // 댓글 알림 전송
            commentCount = article.getCommentCount() + 1;
            article.updateCommentCount(commentCount);
        }

        return ArticleCountResponse.of(articleId,commentCount,commentType);
    }

    /**
     * 댓글 수정 
     * 작성자만이 댓글을 수정 가능 비작성자 수정시 403 예외 발생
     * @param memberId  로그인한 사용자 ID
     * @param commentId 변경할 댓글 ID
     * @param content 수정할 댓글 내용
     */
    @Override
    public void updateArticleComment(Long memberId, Long commentId,String content) {
        ArticleComment articleComment = articleCommentRepository.findById(commentId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
        validWriter(memberId, articleComment);      // 상용자 검증
        articleComment.updateContent(content);      // 댓글 수정
        notificationService.updateNotification(commentId,content);      // 알림 내용 수정
    }

    /**
     * 게시글의 작성된 댓글 조회
     * @param articleId 게시글 아이디
     * @param memberId  사용자 id
     * @param lastId    마지막 댓글 id
     * @param lastLikeCount 마지막 댓글 좋아요 수 
     * @param orderType 정렬 타입
     * @param pageable  페이징 정보
     */
    @Transactional(readOnly = true)
    @Override
    public ArticleCommentResponse getComments(Long articleId, Long memberId, Long lastId,Integer lastLikeCount,OrderType orderType, Pageable pageable) {
        Slice<ArticleCommentDto> allCommentByArticle = articleCommentRepository.getAllCommentByArticle(articleId, memberId, lastId,lastLikeCount,orderType,pageable);
        Integer commentCount = articleCommentRepository.getCommentCount(articleId, memberId);

        return new ArticleCommentResponse(commentCount,allCommentByArticle.hasNext(),allCommentByArticle.getContent());
    }

    /**
     * 댓글의 작성된 대댓글 무한 스크롤 방식으로 조회
     * @param commentId 조회할 댓글 ID
     * @param memberId  로그인한 사용자 ID
     * @param pageable  페이징 정보
     */
    @Transactional(readOnly = true)
    @Override
    public ArticleCommentResponse getReplyComments(Long commentId, Long memberId,Long lastId,Pageable pageable) {
        Slice<ArticleCommentDto> comments = articleCommentRepository.getAllReplyComments(commentId, memberId, lastId,pageable);
        return new ArticleCommentResponse(comments.hasNext(),comments.getContent());
    }

    /**
     * 게시글의 작성된 댓글을 삭제합니다.
     * 부모 댓글 삭제시 : 대댓글과, 좋아요, 댓글과 관련된 알림 기록등을 JDBC 배치를 사용해 삭제합니다.
     * 자식 댓글 삭제시 : 해당 대댓글과 좋아요 대댓글 관련 알림 기록등을 JDBC 배치를 사용해 삭제합니다.
     * 작성자가 아닐시 삭제 요청이오면 403 예외를 발생합니다.
     * @param memberId 로그인한 사용자 ID
     * @param commentId 삭제할 댓글 ID
     * @return ArticleCommentCountResponse 반환
     */
    @Override
    public ArticleCountResponse deleteArticleComment(Long memberId, Long commentId) {
        ArticleComment articleComment = articleCommentRepository.findById(commentId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
        
        //작성자가 아닐시 예외 발생
        validWriter(memberId, articleComment);

        // 부모 댓글을 삭제하려는 경우
        if (articleComment.getParent()==null){
            Article article = articleRepository.findById(articleComment.getArticle().getId()).get();       // 작성된 게시글의 객체를 조호
            int count = article.getCommentCount() - 1;
            article.updateCommentCount(count);  // 부모 댓글 삭제시에는 게시글의 작성된 댓글수 -1
            List<Long> deleteIds = articleCommentRepository.getAllChildComment(commentId);  // 작성된 댓글의 대댓글의 ID를 리스트화
            articleCommentBatchService.deleteCommentBatch(commentId,deleteIds); // 배치를 통해 댓글과 관련된 모든 데이터 삭제

            return ArticleCountResponse.of(article.getId(),count,null);
        }else { // 대댓글을 삭제하는 경우
            articleCommentBatchService.deleteChildCommentBatch(commentId);
            return null;
        }
    }

    /*
    작성자인지 검증하는 메서드
     */
    private static void validWriter(Long memberId, ArticleComment articleComment) {
        if (!articleComment.getMember().getId().equals(memberId)){
            throw new InvalidUsageException(ExceptionStatus.FORBIDDEN,ExceptionType.AUTHOR_ONLY);
        }
    }
}
