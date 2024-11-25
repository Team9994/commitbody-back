package team9499.commitbody.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.file.domain.File;
import team9499.commitbody.domain.file.domain.FileType;
import team9499.commitbody.domain.file.repository.FileRepository;
import team9499.commitbody.global.aws.s3.S3Service;

import static team9499.commitbody.global.constants.ElasticFiled.NO_IMAGE;
import static team9499.commitbody.global.constants.ImageConstants.*;

@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class FileServiceImpl implements FileService{


    private final S3Service s3Service;
    private final FileRepository fileRepository;

    /**
     * 게시글의 데이터 파일을 저장합니다.
     * 파일 유형의 따라 이미지파일 비디오 파일의 타입으로 저장합니다.
     * @param article   게시글
     * @param multipartFile 파일
     */
    @Override
    public String saveArticleFile(Article article, MultipartFile multipartFile) {
        if (multipartFile != null) {
            return handleUploadFile(article, multipartFile);
        }
        return NO_IMAGE;
    }

    /**
     * 게시글의 파일을 수정합니다.
     * 게시글의 업로드한 파일과 동일하지 않을 경우에만 새롭게 저장합니다.
     * @param article 파일의 저장된 게시글
     * @param previousFileName  S3에 저장된 이전 파일명
     * @param multipartFile 파일
     * @return 저장된 파일명 반환
     */
    @Override
    public String updateArticleFile(Article article, String previousFileName, MultipartFile multipartFile) {
        if (multipartFile == null) {
            return null;
        }
        if (previousFileName.isEmpty()) {
            return saveArticleFile(article, multipartFile);
        }
        return handleUpdateFile(article, previousFileName, multipartFile);
    }

    /**
     * 파일의 유형을 검사합니다.
     * @param multipartFile 파일객체
     * @return  사진파일시 IMAGE, 비디오 파일일시 VIDEO
     */
    @Override
    public FileType checkFileType(MultipartFile multipartFile) {
        String contentType = multipartFile.getContentType();
        if (contentType != null && contentType.startsWith(IMAGE)) {
            return FileType.IMAGE;
        }
        return FileType.VIDEO;
    }

    private String handleUploadFile(Article article, MultipartFile multipartFile) {
        String uploadFile = s3Service.uploadFile(multipartFile);
        fileRepository.save(createFile(article, multipartFile, uploadFile, checkFileType(multipartFile)));
        return uploadFile;
    }

    private static File createFile(Article article, MultipartFile file, String storedFilename, FileType fileType) {
        return File.of(file.getOriginalFilename(), storedFilename.substring(INDEX), fileType, article);
    }

    private String handleUpdateFile(Article article, String previousFileName, MultipartFile multipartFile) {
        File file = fileRepository.findByArticleId(article.getId());
        String originalFilename = multipartFile.getOriginalFilename();
        FileType fileType = checkFileType(multipartFile);
        String storedFileName = s3Service.updateFile(multipartFile, previousFileName);
        if ( !file.getOriginName().equals(originalFilename)) {
            file.update(originalFilename, storedFileName.substring(INDEX), fileType);
        }
        return storedFileName;
    }

}
