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
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static team9499.commitbody.global.Exception.ExceptionType.*;
import static team9499.commitbody.global.constants.Delimiter.*;
import static team9499.commitbody.global.constants.ImageConstants.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket.image}")
    private String bucketImage;

    @Value("${cloud.aws.s3.bucket.video}")
    private String bucketVideo;

    @Value("${default.profile-1}")
    private String defaultProfile1;

    @Value("${default.profile-2}")
    private String defaultProfile2;

    @Value("${default.profile-3}")
    private String defaultProfile3;

    @Value("${default.profile-4}")
    private String defaultProfile4;

    @Value("${cloud.aws.cdn.root}")
    private String cdnRoot;

    @Value("${cloud.aws.cdn.url}")
    private String imageUrl;

    @Value("${cloud.aws.cdn.video}")
    private String videoUrl;

    /**
     * 파일 업로드
     * 저장된 파일명을 반환한다.
     */
    @Override
    public String uploadFile(MultipartFile file) {
        if (file != null) {
            Map<String, String> uuid = createUUID(file.getOriginalFilename());
            return uploadAndGenerateFileURL(file, uuid, uuid.get(FILENAME), createObjectMetadata(file));
        }
        return null;
    }

    /**
     * 파일을 업데이트한다. 이전파일을 삭제후 새로운 파일을 저장
     */
    @Override
    public String updateFile(MultipartFile file, String previousFileName) {
        deletePreviousFileIfExists(file, previousFileName);
        return uploadFile(file);
    }

    /**
     * 프로필 사진 업데이트
     * 기본 이미지파일을 경우 이미지파일을 업데이트하고 변경된 프로필 사진을 경우 기존 이미지 파일을 삭제후 새로운 프로플 사진을 업데이트
     */
    @Override
    public String updateProfile(MultipartFile file, String previousFileName, boolean deleteProfile) {
        if (deleteProfile) {     // 기본 프로필 적용시
            applyDefaultProfile(previousFileName);
            return generateRandomProfile();
        }
        if (file != null) {
            handlePreviousProfileDeletion(previousFileName);
            return uploadFile(file);
        }
        return previousFileName;
    }

    @Override
    public String generateRandomProfile() {
        String[] defaultProfiles = {defaultProfile1, defaultProfile2, defaultProfile3, defaultProfile4};
        int randomIndex = new Random().nextInt(defaultProfiles.length);
        return defaultProfiles[randomIndex];
    }

    /**
     * 파일 삭제
     */
    @Override
    public void deleteFile(String fileName, FileType type) {
        try {
            if (type.equals(FileType.IMAGE))
                amazonS3.deleteObject(bucketImage, fileName);
            else if (type.equals(FileType.VIDEO)) {
                amazonS3.deleteObject(bucketVideo, fileName);
            }
        } catch (Exception e) {
            throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR);
        }
    }

    private ObjectMetadata createObjectMetadata(MultipartFile file) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentType(file.getContentType());
        return objectMetadata;
    }

    private String uploadAndGenerateFileURL(MultipartFile file, Map<String, String> uuid, String storedFileName, ObjectMetadata objectMetadata) {
        try {
            InputStream inputStream = file.getInputStream();
            String type = uuid.get(TYPE);
            return uploadFileToS3AndGenerateURL(storedFileName, objectMetadata, type, inputStream);
        } catch (Exception e) {
            throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR);
        }
    }

    private String uploadFileToS3AndGenerateURL(String storedFileName, ObjectMetadata objectMetadata, String type, InputStream inputStream) {
        if (type.equals(MP4) || type.equals(GIF)) {
            amazonS3.putObject(new PutObjectRequest(bucketVideo, storedFileName, inputStream, objectMetadata));
            return videoUrl + storedFileName;
        }
        amazonS3.putObject(new PutObjectRequest(bucketImage, storedFileName, inputStream, objectMetadata));
        return imageUrl + storedFileName;
    }

    private Map<String, String> createUUID(String fileName) {
        String extension = getFileTypeToString(fileName);
        validFileType(extension);
        String uuid = UUID.randomUUID().toString();
        return Map.of(FILENAME, uuid + COMMA + extension, TYPE, extension);
    }

    private void validFileType(String extension) {
        Set<String> allowedExtensions = Set.of(JPEG, JPG, PNG, GIF, MP4);
        if (!allowedExtensions.contains(extension)) {
            throw new InvalidUsageException(ExceptionStatus.BAD_REQUEST, INVALID_FILE_FORMAT);
        }
    }

    private void deletePreviousFileIfExists(MultipartFile file, String previousFileName) {
        if (file != null && previousFileName != null) {
            String extension = getFileTypeToString(previousFileName);
            FileType type = FileType.IMAGE;

            if (extension.equals(MP4) || extension.equals(GIF))
                type = FileType.VIDEO;
            deleteFile(previousFileName, type);
        }
    }

    private String getFileTypeToString(String fileName) {
        return fileName.substring(fileName.lastIndexOf(COMMA) + 1).toLowerCase();
    }

    private void handlePreviousProfileDeletion(String previousFileName) {
        Set<String> profiles = Set.of(defaultProfile1, defaultProfile2, defaultProfile3, defaultProfile4);
        if (!profiles.contains(previousFileName)) {      // 기본 프로필 사진이 아닐 경우 기존 프로필 사진을 삭제
            deleteFile(previousFileName.replace(imageUrl, STRING_EMPTY), FileType.IMAGE);
        }
    }

    private void applyDefaultProfile(String previousFileName) {
        deleteFile(previousFileName.replace(cdnRoot, STRING_EMPTY), FileType.IMAGE);
    }

}
