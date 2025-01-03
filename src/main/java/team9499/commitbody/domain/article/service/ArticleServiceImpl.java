package team9499.commitbody.domain.article.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import team9499.commitbody.domain.article.dto.response.AllArticleResponse;
import team9499.commitbody.domain.article.dto.response.ProfileArticleResponse;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.block.servcice.BlockMemberService;
import team9499.commitbody.domain.file.domain.FileType;
import team9499.commitbody.domain.file.service.FileService;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.global.utils.TimeConverter;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static team9499.commitbody.global.constants.ElasticFiled.NO_IMAGE;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final static String ARTICLE = "article";
    private final static String STORED_NAME = "storedName";
    private final static String VIDEO = "video";

    private final RedisService redisService;
    private final ArticleRepository articleRepository;
    private final FileService fileService;
    private final BlockMemberService blockMemberService;

    @Value("${cloud.aws.cdn.url}")
    private String imageUrl;

    @Value("${cloud.aws.cdn.video}")
    private String videoUrl;

    /**
     * 게시글 전체 조회
     * 인기 게시글 조회시에는 네이트브 쿼리를 사용해 조회하며, 그외 조회는 JPA를 사용해 조회
     *
     * @param memberId        로그인한 사용자 ID
     * @param type            게시글 종류 (운동 인증, 정보 질문)
     * @param articleCategory (전체, 인기, 팔로잉, 장보, 피드백, 몸평)
     * @param lastId          조회된 마지막 게시글 ID
     * @param pageable        페이징 정보
     * @return AllArticleResponse 객체 반환
     */
    @Override
    public AllArticleResponse getAllArticles(Long memberId, ArticleType type, ArticleCategory articleCategory, Long lastId, Pageable pageable) {
        if (articleCategory.equals(ArticleCategory.POPULAR)) {   // 인기 게시글일때
            return new AllArticleResponse(null, false, getAllArticleDtoList(memberId));
        }  // 인기게시글이 아닐때
        Slice<ArticleDto> allArticles = articleRepository.getAllArticles(memberId, type, articleCategory, lastId, pageable);
        return new AllArticleResponse(null, allArticles.hasNext(), allArticles.getContent());
    }

    /**
     * 게시글을 저장합니다.
     *
     * @param memberId        로그인한 사용자 ID
     * @param title           게시글 제목
     * @param content         게시글 내용
     * @param articleType     게시글 타입 (운동인증, 정보&질문)
     * @param articleCategory 정보질문 일때 사용(정보, 피드백, 몸평)
     * @param visibility      게시글 공개 범위(전체, 비공개)
     * @param file            사진
     * @return 저장된 게시글 ID
     */
    @Override
    public ArticleDto saveArticle(Long memberId, String title, String content, ArticleType articleType, ArticleCategory articleCategory, Visibility visibility, MultipartFile file) {
        Member member = getMember(memberId);
        checkCategoryFileType(articleCategory, file);
        Article article = Article.of(title, content, articleType, articleCategory, visibility, member);
        Article save = articleRepository.save(article);
        String filename = fileService.saveArticleFile(article, file);

        return ArticleDto.of(save, member, filename.equals(NO_IMAGE) ? NO_IMAGE : filename);
    }


    /**
     * 작성된 게시글의 대한 상세조회는 메서드
     * 작성자가 차단한 사용자가 조회시 400 예외 발생하며, 타 사용자의 게시글 조회시 차단 여부의 따른 Boolean 타입으로 차단 여부 표시
     *
     * @param memberId  로그인한 사용자 ID
     * @param articleId 조회할 게시글 ID
     * @return ArticleDto 반환
     */
    @Transactional(readOnly = true)
    @Override
    public ArticleDto getDetailArticle(Long memberId, Long articleId) {
        ArticleDto detailArticle = articleRepository.getDetailArticle(memberId, articleId);
        Boolean blockStatus = blockMemberService.checkBlock(detailArticle.getMember().getMemberId(), memberId);
        if (!detailArticle.getMember().getMemberId().equals(memberId))
            detailArticle.getMember().setBlockStatus(blockStatus);

        return detailArticle;
    }

    /**
     * 프로플 페이지에서 해당 사용자가 작성한 게시글을 조회하는 메서드
     *
     * @param loginMemberId 로그인한 사용자 ID
     * @param findMemberId  조회할 사용자 ID
     * @param articleType   게시글 타입
     * @param lastId        조회된 마지막 게시글 ID
     * @param pageable      페이징 정보
     * @return ProfileArticleResponse객체 반환
     */
    @Transactional(readOnly = true)
    @Override
    public ProfileArticleResponse getAllProfileArticle(Long loginMemberId, Long findMemberId, ArticleType articleType, Long lastId, Pageable pageable) {
        Long memberId = loginMemberId.equals(findMemberId) ? loginMemberId : findMemberId;   // 현재 조회할 사용자의 ID를 판별
        Slice<ArticleDto> articles = articleRepository.getAllProfileArticle(loginMemberId, memberId, loginMemberId.equals(findMemberId), articleType, lastId, pageable);
        return new ProfileArticleResponse(articles.hasNext(), articles.getContent());
    }

    /**
     * 게시글을 수정합니다.
     * 작성자가 아닌 사용자가 게시글 수정시 403 예외 발생
     *
     * @param memberId        로그인한 사용자 ID
     * @param articleId       수정할 게시글 ID
     * @param content         수정할 게시글 내용
     * @param articleType     수정시 게시글 타입(운동인증, 정보&질문)
     * @param articleCategory 정보질문 일때 사용(정보, 피드백, 몸평)
     * @param visibility      게시글 공개 범위(전체, 비공개)
     * @param file            – 사진
     */
    @Override
    public ArticleDto updateArticle(Long memberId, Long articleId, String title, String content, ArticleType articleType, ArticleCategory articleCategory, Visibility visibility, MultipartFile file) {
        checkCategoryFileType(articleCategory, file);
        Member member = getMember(memberId);
        Map<String, Object> articleAndFile = articleRepository.getArticleAndFile(articleId);
        Article article = (Article) articleAndFile.get(ARTICLE);
        validWriter(memberId, article);    // 작성자 아닌 사용자가 요청시 403 예외 발생
        article.update(title, content, articleType, articleCategory, visibility);

        String storedFileName = fileService.updateArticleFile(article, (String) articleAndFile.get(STORED_NAME), file);

        return ArticleDto.of(article, member, storedFileName == null ? NO_IMAGE : storedFileName);
    }

    /**
     * 게시글 삭제 : 게시글 삭제시 연관된(좋아요,알림,댓글,파일)을 모두 비동기로 삭제합니다.
     *
     * @param memberId  로그인한 사용자 ID
     * @param articleId 삭제할 게시글 ID
     */
    @Override
    public ArticleDto deleteArticle(Long memberId, Long articleId) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
        ArticleDto articleDto = ArticleDto.of(article, null);
        validWriter(memberId, article);  // 작성자 검증

        articleRepository.deleteArticle(articleId); //비동기를 통한 삭제
        return articleDto;
    }

    private List<ArticleDto> getAllArticleDtoList(Long memberId) {
        List<Object[]> test = articleRepository.findByPopularArticle(memberId);
        return test.stream().map(o -> ArticleDto.of((Long) o[0], (Long) o[8], ArticleCategory.fromKorean((String) o[3]), (String) o[5], (String) o[6], (Integer) o[10], (Integer) o[9],
                        TimeConverter.converter(((Timestamp) o[1]).toLocalDateTime()), converterImgUrl((String) o[12], FileType.valueOf((String) o[15])), (String) o[13], (String) o[14]))
                .collect(Collectors.toList());
    }


    private static void validWriter(Long memberId, Article article) {
        if (!article.getMember().getId().equals(memberId))
            throw new InvalidUsageException(ExceptionStatus.FORBIDDEN, ExceptionType.AUTHOR_ONLY);
    }

    /*
    파일을 S3 CDN의 URL 주소로 변경해줍니다.
     */
    private String converterImgUrl(String storedFileName, FileType type) {
        if (storedFileName == null) {
            return NO_IMAGE;
        }

        return switch (type) {// 파일 타입에 따른 URL 생성
            case IMAGE -> imageUrl + storedFileName;
            case VIDEO -> videoUrl + storedFileName;
            case DEFAULT -> null;
        };
    }

    /*
    카테고리가 몸평이 아닌 경우에는 이미지만 등록가능 하도록 400 예외 발생
     */
    private static void checkCategoryFileType(ArticleCategory articleCategory, MultipartFile file) {
        if (articleCategory != null && !articleCategory.equals(ArticleCategory.BODY_REVIEW) && file != null) {
            if (file.getContentType().contains(VIDEO))
                throw new InvalidUsageException(ExceptionStatus.BAD_REQUEST, ExceptionType.ONLY_IMAGE);
        }
    }

    private Member getMember(Long memberId) {
        return redisService.getMemberDto(String.valueOf(memberId)).get();
    }
}
