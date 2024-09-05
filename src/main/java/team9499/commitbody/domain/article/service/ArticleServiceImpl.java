package team9499.commitbody.domain.article.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
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
}
