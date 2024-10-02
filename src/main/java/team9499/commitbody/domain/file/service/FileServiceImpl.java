package team9499.commitbody.domain.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.file.domain.File;
import team9499.commitbody.domain.file.domain.FileType;
import team9499.commitbody.domain.file.repository.FileRepository;
import team9499.commitbody.global.aws.s3.S3Service;

@Slf4j
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
        String storedFilename = null;
        if (multipartFile != null) {
            storedFilename = s3Service.uploadImage(multipartFile);
            String originalFilename = multipartFile.getOriginalFilename();
            FileType fileType = checkFileType(multipartFile);
            File file = File.of(originalFilename, storedFilename.substring(45), fileType, article);
            fileRepository.save(file);
        }else storedFilename = "등록된 이미지가 없습니다.";

        return storedFilename;
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
        // 파일이 없을시 저장 안함
        if (multipartFile == null) {
            return null;
        }

        // 파일명이 "" 일경우 새롭게 파일을 저장
        if (previousFileName.isEmpty()) {
            saveArticleFile(article, multipartFile);
            return null;
        }

        File file = fileRepository.findByArticleId(article.getId());
        String originalFilename = multipartFile.getOriginalFilename();
        FileType fileType = checkFileType(multipartFile);
        String storedFileName = s3Service.updateImage(multipartFile, previousFileName);

        if ( !file.getOriginName().equals(originalFilename)) {
            file.update(originalFilename, storedFileName.substring(45), fileType);
        }
        
        return storedFileName;
    }


    /**
     * 파일의 유형을 검사합니다.
     * @param multipartFile 파일객체
     * @return  사진파일시 IMAGE, 비디오 파일일시 VIDEO
     */
    @Override
    public FileType checkFileType(MultipartFile multipartFile) {
        String contentType = multipartFile.getContentType();
        FileType fileType = FileType.DEFAULT;
        if (contentType!=null && contentType.startsWith("image/"))
            fileType = FileType.IMAGE;
        else if (fileType!=null && contentType.startsWith("video/"))
            fileType = FileType.VIDEO;

        return fileType;
    }
}
