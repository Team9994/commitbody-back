package team9499.commitbody.domain.file.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.file.domain.File;
import team9499.commitbody.domain.file.domain.FileType;
import team9499.commitbody.domain.file.repository.FileRepository;
import team9499.commitbody.global.aws.s3.S3Service;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock private S3Service s3Service;
    @Mock private FileRepository fileRepository;
    @InjectMocks private FileServiceImpl fileService;

    private String url = "https://aaaaaaaaaaaaaa.cloudfront.net/images/";
    
    @DisplayName("파일 저장 - 파일 존재시")
    @Test
    void saveFileIsNotNull(){
        String uuid = url+"35017bde-8037-43c4-a53a-f4d5f7dbe1a3.png";
        File file = new File();
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "file", "image/jpeg", "test".getBytes(StandardCharsets.UTF_8));
        when(s3Service.uploadFile(eq(mockMultipartFile))).thenReturn(uuid);
        when(fileRepository.save(any())).thenReturn(file);
        String fileName = fileService.saveArticleFile(new Article(), mockMultipartFile);

        assertThat(fileName).isEqualTo(uuid);
    }


    @DisplayName("파일 저장 - 파일 미존재시")
    @Test
    void saveFileIsNull(){
        String fileName = fileService.saveArticleFile(new Article(), null);
        assertThat(fileName).isEqualTo("등록된 이미지 파일이 없습니다.");
    }
    
    @DisplayName("파일 업데이트 = 이전 파일이 없을 경우")
    @Test
    void updateFileBeforeNull(){

        String uuid = url+"35017bde-8037-43c4-a53a-f4d5f7dbe1a3.png";
        File file = new File();
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "file", "image/jpeg", "test".getBytes(StandardCharsets.UTF_8));
        when(s3Service.uploadFile(eq(mockMultipartFile))).thenReturn(uuid);
        when(fileRepository.save(any())).thenReturn(file);

        String updateFileName = fileService.updateArticleFile(new Article(), "", mockMultipartFile);
        assertThat(updateFileName).isEqualTo(uuid);
    }
    
    @DisplayName("파일 업데이트 = 이전 파일이 존재할경우")
    @Test
    void updateFileIsNotNull(){
        Article article = Article.builder().id(1L).build();
        String before = url+"before.png";
        String after = url+"new.png";
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "file", "image/jpeg", "test".getBytes(StandardCharsets.UTF_8));
        File file = File.of("notSame", after, FileType.IMAGE, article);

        when(fileRepository.findByArticleId(anyLong())).thenReturn(file);
        when(s3Service.updateFile(eq(mockMultipartFile),eq(before))).thenReturn(after);

        String updateFilename = fileService.updateArticleFile(article, before, mockMultipartFile);
        assertThat(updateFilename).isEqualTo(after);
    }

    
    @DisplayName("파일 유형 체크")
    @Test
    void fileTypeCheck(){
        MockMultipartFile imageMockFile = new MockMultipartFile("file", "file", "image/jpeg", "test".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile videoMockFile = new MockMultipartFile("file", "file", "video/mp4", "test".getBytes(StandardCharsets.UTF_8));

        FileType imageType = fileService.checkFileType(imageMockFile);
        FileType videoType = fileService.checkFileType(videoMockFile);

        assertThat(imageType).isEqualTo(FileType.IMAGE);
        assertThat(videoType).isEqualTo(FileType.VIDEO);

    }



}