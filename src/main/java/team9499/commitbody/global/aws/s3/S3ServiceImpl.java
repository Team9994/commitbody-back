package team9499.commitbody.global.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.ServerException;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import static team9499.commitbody.global.Exception.ExceptionType.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service{

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket.image}")
    private String bucketImage;

    @Value("${default.profile}")
    private String defaultProfile;

    @Value("${cloud.aws.cdn.url}")
    private String cdnUrl;

    /**
     * 파일 업로드
     * 저장된 파일명을 반환한다.
     */
    @Override
    public String uploadImage(MultipartFile file) {
        String storedFileName =null;
        if (file!=null) {
            String originalFilename = file.getOriginalFilename();

            storedFileName = createUUID(originalFilename);
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            try {
                InputStream inputStream = file.getInputStream();
                amazonS3.putObject(new PutObjectRequest(bucketImage, storedFileName, inputStream, objectMetadata));
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR);
            }
        }

        return storedFileName;
    }

    /**
     * 파일을 업데이트한다. 이전파일을 삭제후 새로운 파일을 저장
     */
    @Override
    public String updateImage(MultipartFile file, String previousFileName) {
        String previous = previousFileName;
        if (file!=null) {
            deleteImage(previousFileName);
            previous = uploadImage(file);
        }
        return previous;
    }

    /**
     * 프로필 사진 업데이트
     * 기본 이미지파일을 경우 이미지파일을 업데이트하고 변경된 프로필 사진을 경우 기존 이미지 파일을 삭제후 새로운 프로플 사진을 업데이트
     * @param file
     * @param previousFileName  이전 프로필 파일 이름
     * @return
     */
    @Override
    public String updateProfile(MultipartFile file, String previousFileName,boolean deleteProfile) {
        String previous = previousFileName;
        if (deleteProfile){     // 기본 프로필 적용시
            previous = defaultProfile;
            deleteImage(previousFileName.replace(cdnUrl, ""));
        }
        if (file!=null) {
            if (!previous.equals(defaultProfile)){      // 기본 프로필 사진이 아닐 경우 기존 프로필 사진을 삭제
                deleteImage(previousFileName.replace(cdnUrl, ""));
            }
            previous = cdnUrl+uploadImage(file);
        }

        return previous;
    }

    /**
     * 파일 삭제
     */
    @Override
    public void deleteImage(String fileName) {
        try {
            amazonS3.deleteObject(bucketImage,fileName);
        }catch (Exception e){
            log.error("이미지 삭제중 오류 발생");
            throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR,SERVER_ERROR);
        }
    }




    private String createUUID(String fileName){
        int indexOf = fileName.lastIndexOf(".");
        String extension = fileName.substring(indexOf + 1).toLowerCase();

        Set<String> allowedExtensions = Set.of("jpeg", "jpg", "png","gif");
        if (!allowedExtensions.contains(extension)){
            throw new InvalidUsageException(ExceptionStatus.BAD_REQUEST, INVALID_IMAGE_FORMAT);
        }
        String uuid = UUID.randomUUID().toString();
        return uuid+"."+extension;
    }
}
