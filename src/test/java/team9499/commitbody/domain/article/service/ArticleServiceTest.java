package team9499.commitbody.domain.article.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.domain.LoginType;
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
import team9499.commitbody.domain.file.service.FileService;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.mock.MockUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock private RedisService redisService;
    @Mock private ArticleRepository articleRepository;
    @Mock private FileService fileService;
    @Mock private BlockMemberService blockMemberService;

    @InjectMocks private ArticleServiceImpl articleService;

    private final Long memberId = 1l;
    private final Long articleId = 2l;
    private Member member;
    private Article article;

    @BeforeEach
    void init(){
        member = Member.builder().id(memberId).loginType(LoginType.KAKAO).nickname("사용자").build();
        article =Article.builder().id(articleId).title("제목").content("내용").articleType(ArticleType.INFO_QUESTION)
                .articleCategory(ArticleCategory.FEEDBACK).visibility(Visibility.PUBLIC).member(member).build();
    }

    @DisplayName("게시글 저장")
    @Test
    void saveArticle() {

        whenMemberDTO();

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "test", "image/png", "test".getBytes());

        Article article = Article.of("제목","내용",ArticleType.EXERCISE,null,Visibility.PUBLIC,member);

        when(articleRepository.save(any())).thenReturn(article);
        when(fileService.saveArticleFile(any(Article.class), any(MultipartFile.class))).thenReturn("test-file-name");
        
        ArticleDto articleDto = articleService.saveArticle(memberId, "제목", "내용", ArticleType.EXERCISE, null, Visibility.PUBLIC, mockMultipartFile);
        
        assertThat(articleDto.getMember().getNickname()).isEqualTo("사용자");
        assertThat(articleDto.getImageUrl()).isEqualTo("test-file-name");
    }

    @DisplayName("몸평 카테고리 제외 - 비디오 저장 예외 발생")
    @Test
    void saveThrowVideo(){
        whenMemberDTO();

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "test", "video/mp4", "test".getBytes());

        assertThatThrownBy(() -> articleService.saveArticle(memberId, "제목", "내용", ArticleType.INFO_QUESTION, ArticleCategory.FEEDBACK, Visibility.PUBLIC, mockMultipartFile)).isInstanceOf(InvalidUsageException.class).hasMessage("이미지 파일만 등록 가능합니다.");
    }

    @DisplayName("게시글 수정")
    @Test
    void updateArticle(){
        whenMemberDTO();

        Map<String,Object> map = Map.of("article",article,"storedName","test-file_name");
        when(articleRepository.getArticleAndFile(anyLong())).thenReturn(map);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "exchange-file-name", "image/png", "test".getBytes());
        when(fileService.updateArticleFile(any(),anyString(),any())).thenReturn("exchange-file-name");

        ArticleDto articleDto = articleService.updateArticle(memberId, articleId, "변경 제목", "변경 내용", ArticleType.INFO_QUESTION, ArticleCategory.FEEDBACK, Visibility.PUBLIC, mockMultipartFile);
        
        assertThat(articleDto.getContent()).isEqualTo("변경 내용");
        assertThat(articleDto.getTitle()).isNotEqualTo("제목");
        assertThat(articleDto.getImageUrl()).isEqualTo("exchange-file-name");
    }

    @DisplayName("작성자가 아닐시 수정 기능 사용시 예외")
    @MockUser
    @Test
    void writerValidThrow(){
        whenMemberDTO();
        Map<String,Object> map = Map.of("article",article,"storedName","test-file_name");
        when(articleRepository.getArticleAndFile(anyLong())).thenReturn(map);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "exchange-file-name", "image/png", "test".getBytes());

        assertThatThrownBy(()->articleService.updateArticle(2l, articleId, "변경 제목", "변경 내용", ArticleType.INFO_QUESTION, ArticleCategory.FEEDBACK, Visibility.PUBLIC, mockMultipartFile))
                .hasMessage("작성자만 이용할 수 있습니다.")
                .isInstanceOf(InvalidUsageException.class);
    }

    
    @DisplayName("게시글 삭제")
    @Test
    void deleteArticle(){
        when(articleRepository.findById(anyLong())).thenReturn(Optional.ofNullable(article));

        articleService.deleteArticle(memberId,articleId);

        verify(articleRepository, times(1)).deleteArticle(articleId);
    }


    @DisplayName("게시글 상세조회")
    @Test
    void detailsArticle(){
        ArticleDto articleDto = ArticleDto.of(article, member, "testUrl");

        when(articleRepository.getDetailArticle(anyLong(),anyLong())).thenReturn(articleDto);
        when(blockMemberService.checkBlock(anyLong(),eq(memberId))).thenReturn(false);

        ArticleDto detailArticle = articleService.getDetailArticle(memberId, articleId);
        
        assertThat(detailArticle.getTitle()).isEqualTo("제목");
        assertThat(detailArticle.getContent()).isEqualTo("내용");
    }

    @DisplayName("게시글 전제 조회")
    @Test
    void getAllArticle(){
        Pageable pageable = Pageable.ofSize(2);
        List<ArticleDto> content = List.of(ArticleDto.of(article,member,"test"));
        SliceImpl<ArticleDto> articleDtos = new SliceImpl<>(content,pageable , false);
        when(articleRepository.getAllArticles(anyLong(),any(),any(),isNull(),any(Pageable.class))).thenReturn(articleDtos);

        AllArticleResponse allArticles = articleService.getAllArticles(memberId, ArticleType.INFO_QUESTION, ArticleCategory.FEEDBACK, null, pageable);
        assertThat(allArticles.getArticles().size()).isEqualTo(1);
        assertThat(allArticles.isHasNext()).isFalse();

    }

    @DisplayName("프로필 게시글 조회")
    @Test
    void profileArticle(){
        Pageable pageable = Pageable.ofSize(2);

        List<ArticleDto> content = List.of(ArticleDto.of(article,member,"test"));
        SliceImpl<ArticleDto> articleDtos = new SliceImpl<>(content,pageable , false);
        when(articleRepository.getAllProfileArticle(anyLong(),anyLong(),eq(true),eq(ArticleType.INFO_QUESTION),isNull(),any(Pageable.class))).thenReturn(articleDtos);

        ProfileArticleResponse allProfileArticle = articleService.getAllProfileArticle(memberId, memberId, ArticleType.INFO_QUESTION, null, pageable);
        assertThat(allProfileArticle.getArticles().size()).isEqualTo(1);
        assertThat(allProfileArticle.isHasNext()).isFalse();
    }


    private void whenMemberDTO() {
        when(redisService.getMemberDto(anyString())).thenReturn(Optional.of(member));
    }


}