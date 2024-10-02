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
import team9499.commitbody.domain.file.domain.FileType;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.ServerException;

import java.io.InputStream;
import java.util.Map;
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

    @Value("${cloud.aws.s3.bucket.video}")
    private String bucketVideo;

    @Value("${default.profile}")
    private String defaultProfile;

    @Value("${cloud.aws.cdn.url}")
    private String imageUrl;

    @Value("${cloud.aws.cdn.video}")
    private String videoUrl;

    /**
     * 파일 업로드
     * 저장된 파일명을 반환한다.
     */
    @Override
    public String uploadImage(MultipartFile file) {
        String storedFileName =null;
        if (file!=null) {
            String originalFilename = file.getOriginalFilename();

            Map<String, String> uuid = createUUID(originalFilename);
            storedFileName = uuid.get("fileName");
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            try {
                InputStream inputStream = file.getInputStream();
                String type = uuid.get("type");
                if (type.equals("mp4") || type.equals("gif")){
                    amazonS3.putObject(new PutObjectRequest(bucketVideo, storedFileName, inputStream, objectMetadata));
                    storedFileName = videoUrl + storedFileName;
                }else{
                    amazonS3.putObject(new PutObjectRequest(bucketImage, storedFileName, inputStream, objectMetadata));
                    storedFileName = imageUrl + storedFileName;
                }

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
        if (file!=null && previousFileName!=null) {
            String fileName = file.getOriginalFilename();
            String extension = getFileTypeToString(fileName);
            FileType type = FileType.IMAGE;

            if (extension.equals("mp4") || extension.equals("gif")) type = FileType.VIDEO;
            deleteImage(previousFileName,type);
        }
        return uploadImage(file);
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
            deleteImage(previousFileName.replace(imageUrl, ""),FileType.IMAGE);
        }
        if (file!=null) {
            if (!previous.equals(defaultProfile)){      // 기본 프로필 사진이 아닐 경우 기존 프로필 사진을 삭제
                deleteImage(previousFileName.replace(imageUrl, ""),FileType.IMAGE);
            }
            previous = imageUrl +uploadImage(file);
        }

        return previous;
    }

    /**
     * 파일 삭제
     */
    @Override
    public void deleteImage(String fileName,FileType type) {
        try {
            if (type.equals(FileType.IMAGE))
                amazonS3.deleteObject(bucketImage,fileName);
            else if (type.equals(FileType.VIDEO)){
                amazonS3.deleteObject(bucketVideo,fileName);
            }
        }catch (Exception e){
            log.error("이미지 삭제중 오류 발생");
            throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR,SERVER_ERROR);
        }
    }

    private static String getFileTypeToString(String fileName) {
        int indexOf = fileName.lastIndexOf(".");
        return fileName.substring(indexOf + 1).toLowerCase();
    }


    private Map<String,String> createUUID(String fileName){
        String extension = getFileTypeToString(fileName);

        Set<String> allowedExtensions = Set.of("jpeg", "jpg", "png","gif","mp4");
        if (!allowedExtensions.contains(extension)){
            throw new InvalidUsageException(ExceptionStatus.BAD_REQUEST, INVALID_IMAGE_FORMAT);
        }
        String uuid = UUID.randomUUID().toString();
        return Map.of("fileName",uuid+"."+extension,"type",extension);
    }
}
