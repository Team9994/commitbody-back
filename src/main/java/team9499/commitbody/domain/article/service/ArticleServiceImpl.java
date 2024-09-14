package team9499.commitbody.domain.article.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.dto.response.ProfileArticleResponse;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.block.servcice.BlockMemberService;
import team9499.commitbody.domain.file.service.FileService;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.redis.RedisService;

import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService{

    private final RedisService redisService;
    private final ArticleRepository articleRepository;
    private final FileService fileService;
    private final BlockMemberService blockMemberService;


    /**
     * 게시글을 저장합니다.
     * @param memberId  로그인한 사용자 ID
     * @param title 게시글 제목
     * @param content   게시글 내용
     * @param articleType   게시글 타입 (운동인증, 정보&질문)
     * @param articleCategory   정보질문 일때 사용(정보, 피드백, 몸평)
     * @param visibility 게시글 공개 범위(전체, 비공개)
     * @param file 사진
     * @return  저장된 게시글 ID
     */
    @Override
    public Long saveArticle(Long memberId, String title, String content, ArticleType articleType, ArticleCategory articleCategory, Visibility visibility, MultipartFile file) {
        Member member = redisService.getMemberDto(String.valueOf(memberId)).get();
        Article article = Article.of(title,content,articleType,articleCategory,visibility,member);
        articleRepository.save(article);
        fileService.saveArticleFile(article,file);
        return article.getId();
    }

    /**
     * 작성된 게시글의 대한 상세조회는 메서드
     * 작성자가 차단한 사용자가 조회시 400 예외 발생하며, 타 사용자의 게시글 조회시 차단 여부의 따른 Boolean 타입으로 차단 여부 표시
     * @param memberId  로그인한 사용자 ID
     * @param articleId 조회할 게시글 ID
     * @return ArticleDto 반환
     */
    @Transactional(readOnly = true)
    @Override
    public ArticleDto getDetailArticle(Long memberId, Long articleId) {
        ArticleDto detailArticle = articleRepository.getDetailArticle(memberId, articleId);
        // 차단 여부 체크
        Boolean blockStatus = blockMemberService.checkBlock(detailArticle.getMember().getMemberId(),memberId);
        if (!detailArticle.getMember().getMemberId().equals(memberId))
            detailArticle.getMember().setBlockStatus(blockStatus);

        return detailArticle;
    }

    /**
     * 프로플 페이지에서 해당 사용자가 작성한 게시글을 조회하는 메서드
     * @param loginMemberId 로그인한 사용자 ID
     * @param findMemberId  조회할 사용자 ID
     * @param articleType   게시글 타입
     * @param lastId    조회된 마지막 게시글 ID
     * @param pageable  페이징 정보
     * @return  ProfileArticleResponse객체 반환
     */
    @Transactional(readOnly = true)
    @Override
    public ProfileArticleResponse getAllProfileArticle(Long loginMemberId, Long findMemberId, ArticleType articleType , Long lastId, Pageable pageable) {
        Long memberId = loginMemberId == findMemberId ? loginMemberId : findMemberId;   // 현재 조회할 사용자의 ID를 판별
        boolean myAccount = loginMemberId == findMemberId  ? true : false;  // 현자 나의 프로필인지 확인
        Slice<ArticleDto> articles = articleRepository.getAllProfileArticle(loginMemberId, memberId,myAccount, articleType,lastId, pageable);
        return new ProfileArticleResponse(articles.hasNext(),articles.getContent());
    }

    /**
     * 게시글을 수정합니다.
     * 작성자가 아닌 사용자가 게시글 수정시 403 예외 발생
     * @param memberId  로그인한 사용자 ID
     * @param articleId 수정할 게시글 ID
     * @param content   수정할 게시글 내용
     * @param articleType   수정시 게시글 타입(운동인증, 정보&질문)
     * @param articleCategory 정보질문 일때 사용(정보, 피드백, 몸평)
     * @param visibility 게시글 공개 범위(전체, 비공개)
     * @param file  – 사진
     */
    @Override
    public void updateArticle(Long memberId, Long articleId, String title , String content, ArticleType articleType, ArticleCategory articleCategory, Visibility visibility, MultipartFile file) {
        Map<String, Object> articleAndFile = articleRepository.getArticleAndFile(articleId);
        Article article = (Article)articleAndFile.get("article");
        String previousFileName = (String) articleAndFile.get("storedName");

        // 작성자 아닌 사용자가 요청시 403 예외 발생
        validWriter(memberId, article);
        article.update(title, content, articleType, articleCategory, visibility);

        fileService.updateArticleFile(article, previousFileName, file);


    }

    /**
     * 게시글 삭제 : 게시글 삭제시 연관된(좋아요,알림,댓글,파일)을 모두 비동기로 삭제합니다.
     * @param memberId 로그인한 사용자 ID
     * @param articleId 삭제할 게시글 ID
     */
    @Override
    public void deleteArticle(Long memberId, Long articleId) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
        validWriter(memberId,article);  // 작성자 검증

        articleRepository.deleteArticle(articleId); //비동기를 통한 삭제
    }

    private static void validWriter(Long memberId, Article article) {
        if (!article.getMember().getId().equals(memberId))
            throw new InvalidUsageException(ExceptionStatus.FORBIDDEN, ExceptionType.AUTHOR_ONLY);
    }
}
