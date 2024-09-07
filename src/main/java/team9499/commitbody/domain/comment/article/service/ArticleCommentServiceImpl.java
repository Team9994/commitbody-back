package team9499.commitbody.domain.comment.article.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;
import team9499.commitbody.domain.comment.article.dto.response.ArticleCommentResponse;
import team9499.commitbody.domain.comment.article.repository.ArticleCommentRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.notification.service.NotificationService;
import team9499.commitbody.global.redis.RedisService;



@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ArticleCommentServiceImpl implements ArticleCommentService{

    private final ArticleCommentRepository articleCommentRepository;
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
     */
    @Override
    public String saveArticleComment(Long memberId, Long articleId, Long commentParentId, String content,String replyNickname) {
        // 게시글 조회
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));

        // 댓글 작성자 정보 조회
        Member member = redisService.getMemberDto(String.valueOf(memberId))
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));

        ArticleComment articleComment;
        String commentType ="";

        // 부모 댓글이 존재하는 경우 대댓글로 처리
        if (commentParentId != null) {
            ArticleComment parentComment = articleCommentRepository.getReferenceById(commentParentId);
            articleComment = ArticleComment.of(article, member, content, parentComment); // 부모 댓글을 포함해 대댓글 생성

            ArticleComment save = articleCommentRepository.save(articleComment);// 댓글 저장
            parentComment.addChildComment(articleComment); // 부모 댓글에 대댓글 추가
            commentType = "대댓글 작성 성공";
            // 비동기를 통한 알림 전송
            if (replyNickname!=null)
                notificationService.sendReplyComment(member,replyNickname, article.getTitle(),parentComment.getContent(),String.valueOf(save.getId()));
        } else { // 부모 댓글인 경우
            articleComment = ArticleComment.of(article, member, content, null); // 부모 댓글 없이 최상위 댓글 생성
            ArticleComment save = articleCommentRepository.save(articleComment);// 댓글 저장
            commentType ="댓글 작성 성공";
            notificationService.sendComment(member,article.getMember().getId(),article.getTitle(),content,String.valueOf(save.getId()));    // 댓글 알림 전송
        }
        article.updateCommentCount(article.getCommentCount()+1);
        return commentType;
    }

    /**
     * 게시글의 작성된 댓글 조회
     * @param articleId 게시글 아이디
     * @param memberId  사용자 id
     * @param lastId    마지막 댓글 id
     * @param lastLikeCount 마지막 댓글 좋아요 수 
     * @param orderType 정렬 타입
     * @param pageable  페이징 정보
     * @return
     */
    @Transactional(readOnly = true)
    @Override
    public ArticleCommentResponse getComments(Long articleId, Long memberId, Long lastId,Integer lastLikeCount,OrderType orderType, Pageable pageable) {

        Slice<ArticleCommentDto> allCommentByArticle = articleCommentRepository.getAllCommentByArticle(articleId, memberId, lastId,lastLikeCount,orderType,pageable);

        return new ArticleCommentResponse(allCommentByArticle.hasNext(),allCommentByArticle.getContent());
    }
}
