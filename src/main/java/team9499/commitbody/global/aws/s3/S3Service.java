package team9499.commitbody.global.aws.s3;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    String uploadImage(MultipartFile file);
    String updateImage(MultipartFile file, String previousFileName);
    String updateProfile(MultipartFile file, String previousFileName, boolean deleteProfile);
    void deleteImage(String fileName);
}
