package team9499.commitbody.global.aws.s3;

import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.file.domain.FileType;

public interface S3Service {

    String uploadFile(MultipartFile file);

    String updateFile(MultipartFile file, String previousFileName);

    String updateProfile(MultipartFile file, String previousFileName, boolean deleteProfile);

    void deleteFile(String fileName, FileType type);
}
