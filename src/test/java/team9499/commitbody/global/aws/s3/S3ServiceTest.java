package team9499.commitbody.global.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import team9499.commitbody.domain.file.domain.FileType;
import team9499.commitbody.global.Exception.InvalidUsageException;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = S3ServiceImpl.class)
@TestPropertySource
class S3ServiceTest {

    @Mock private AmazonS3 amazonS3;

    @InjectMocks private S3ServiceImpl s3Service;

    String root = "https://ClodFronTest.url/";
    String imageUrl = "https://ClodFronTest.url/image/";
    String defaultProfile1 = "https://ClodFronTest.url/default-1.jpeg";
    String defaultProfile2 = "https://ClodFronTest.url/default-2.jpeg";
    String defaultProfile3 = "https://ClodFronTest.url/default-3.jpeg";
    String defaultProfile4 = "https://ClodFronTest.url/default-4.jpeg";
    String bucketImage = "https://buket/image";
    String bucketVideo = "https://buket/video";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service,"bucketImage",bucketImage);
        ReflectionTestUtils.setField(s3Service,"bucketVideo",bucketVideo);
        ReflectionTestUtils.setField(s3Service,"imageUrl",imageUrl);
        ReflectionTestUtils.setField(s3Service,"defaultProfile1",defaultProfile1);
        ReflectionTestUtils.setField(s3Service,"defaultProfile2",defaultProfile2);
        ReflectionTestUtils.setField(s3Service,"defaultProfile3",defaultProfile3);
        ReflectionTestUtils.setField(s3Service,"defaultProfile4",defaultProfile4);
        ReflectionTestUtils.setField(s3Service,"cdnRoot",root);
    }


    @DisplayName("S3 업로드 - 이미지")
    @Test
    void uploadS3ImageFile(){
        MockMultipartFile mockMultipartFile = new MockMultipartFile("imageFile", "사진파일.jpeg", "image/jpeg", "file".getBytes(StandardCharsets.UTF_8));
        PutObjectResult mock = mock(PutObjectResult.class);

        when(amazonS3.putObject(any())).thenReturn(mock);

        String filename = s3Service.uploadFile(mockMultipartFile);;
        assertThat(filename).isNotEmpty();
        assertThat(filename).contains("jpeg");
        assertThat(filename).isInstanceOf(String.class);
    }

    @DisplayName("S3 업로드 - 영상")
    @Test
    void uploadS3VideoFile(){
        MockMultipartFile mockMultipartFile = new MockMultipartFile("imageFile", "영상파일.mp4", "video/mp4", "file".getBytes(StandardCharsets.UTF_8));
        PutObjectResult mock = mock(PutObjectResult.class);

        when(amazonS3.putObject(any())).thenReturn(mock);

        String filename = s3Service.uploadFile(mockMultipartFile);;
        assertThat(filename).isNotEmpty();
        assertThat(filename).contains("mp4");
        assertThat(filename).isInstanceOf(String.class);
    }
    
    @DisplayName("S3 업로드 - 사용할수 없는 형식을 경우 예외 발생")
    @Test
    void uploadS3NotUseTypeException(){
        MockMultipartFile mockMultipartFile = new MockMultipartFile("imageFile", "영상파일.aaa", "video/aaa", "file".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> s3Service.uploadFile(mockMultipartFile)).isInstanceOf(InvalidUsageException.class).hasMessage("올바른 파일 형식이 아닙니다.");
    }

    
    @DisplayName("S3 업데이트 - 이전 파일 존재시")
    @Test
    void updateS3FileWhenOldFileExists(){
        MockMultipartFile mockMultipartFile = new MockMultipartFile("imageFile", "새로운_사진파일.jpeg", "image/jpeg", "file content".getBytes(StandardCharsets.UTF_8));

        String previousFileName = "이전_사진파일.jpeg";
        PutObjectResult mockPutResult = mock(PutObjectResult.class);
        doNothing().when(amazonS3).deleteObject(any(), eq(previousFileName));
        when(amazonS3.putObject(any(PutObjectRequest.class))).thenReturn(mockPutResult);
        
        String updatedFileUrl = s3Service.updateFile(mockMultipartFile, previousFileName);

        assertThat(updatedFileUrl).isNotEmpty();
        verify(amazonS3,times(1)).deleteObject(eq(bucketImage),anyString());
    }

    @DisplayName("S3 업데이트 - 이전 파일 미존재시")
    @Test
    void updateS3FileWhenOldFileNotExists(){
        MockMultipartFile mockMultipartFile = new MockMultipartFile("imageFile", "새로운_사진파일.jpeg", "image/jpeg", "file content".getBytes(StandardCharsets.UTF_8));

        PutObjectResult mockPutResult = mock(PutObjectResult.class);
        when(amazonS3.putObject(any(PutObjectRequest.class))).thenReturn(mockPutResult);

        String updatedFileUrl = s3Service.updateFile(mockMultipartFile, null);

        assertThat(updatedFileUrl).isNotEmpty();
        verify(amazonS3,times(0)).deleteObject(any(),anyString());
    }
    
    @DisplayName("S3 프로필 사진업데이트 - 새로운 프로필 변경")
    @Test
    void updateS3NewProfile(){
        String preFile = "커스텀 사용자 프로필.jpeg";
        PutObjectResult mockPutResult = mock(PutObjectResult.class);
        MockMultipartFile mockMultipartFile = new MockMultipartFile("imageFile", "새로운_프로필_사진파일.jpeg", "image/jpeg", "file content".getBytes(StandardCharsets.UTF_8));

        doNothing().when(amazonS3).deleteObject(any(),anyString());
        when(amazonS3.putObject(any(PutObjectRequest.class))).thenReturn(mockPutResult);

        String newProFile = s3Service.updateProfile(mockMultipartFile, preFile, false);
        assertThat(newProFile).isNotEmpty();
        assertThat(newProFile).contains("jpeg");

        verify(amazonS3,times(1)).deleteObject(eq(bucketImage),anyString());
    }

    @DisplayName("S3 프로필 사진업데이트 - 기본 프로필 변경")
    @Test
    void updateS3DefaultProfile(){
        String preFile = "커스텀 사용자 프로필.jpeg";
        Set<String> defaultProfileSET = Set.of(defaultProfile1,defaultProfile2,defaultProfile3,defaultProfile4);
        doNothing().when(amazonS3).deleteObject(any(),anyString());

        String defaultProFile = s3Service.updateProfile(null, preFile, true);
        assertThat(defaultProfileSET).contains(defaultProFile);

        verify(amazonS3,times(1)).deleteObject(any(),anyString());
        verify(amazonS3,times(0)).putObject(any(),anyString(),anyString());
    }

    @DisplayName("S3 파일 삭제 - 이미지")
    @Test
    void deleteS3Image(){
        doNothing().when(amazonS3).deleteObject(eq(bucketImage),anyString());

        s3Service.deleteFile("사진.jpeg", FileType.IMAGE);

        verify(amazonS3,times(1)).deleteObject(eq(bucketImage),anyString());
        verify(amazonS3,times(0)).deleteObject(eq(bucketVideo),anyString());
    }

    @DisplayName("S3 파일 삭제 - 동영상")
    @Test
    void deleteS3Video(){
        doNothing().when(amazonS3).deleteObject(eq(bucketVideo),anyString());

        s3Service.deleteFile("동영상.mp4", FileType.VIDEO);

        verify(amazonS3,times(0)).deleteObject(eq(bucketImage),anyString());
        verify(amazonS3,times(1)).deleteObject(eq(bucketVideo),anyString());
    }


}