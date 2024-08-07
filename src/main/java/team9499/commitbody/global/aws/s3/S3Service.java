package team9499.commitbody.global.aws.s3;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    String uploadImage(MultipartFile file);
    String updateImage(MultipartFile file, String previousFileName);
    void deleteImage(String fileName);
}
