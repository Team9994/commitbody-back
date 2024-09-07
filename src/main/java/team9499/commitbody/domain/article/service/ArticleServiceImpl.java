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
import team9499.commitbody.domain.file.service.FileService;
import team9499.commitbody.global.redis.RedisService;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService{

    private final RedisService redisService;
    private final ArticleRepository articleRepository;
    private final FileService fileService;


    @Override
    public Long saveArticle(Long memberId, String title, String content, ArticleType articleType, ArticleCategory articleCategory, Visibility visibility, MultipartFile file) {
        Member member = redisService.getMemberDto(String.valueOf(memberId)).get();
        Article article = Article.of(title,content,articleType,articleCategory,visibility,member);
        articleRepository.save(article);
        fileService.saveArticleFile(article,file);
        return article.getId();
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
}
