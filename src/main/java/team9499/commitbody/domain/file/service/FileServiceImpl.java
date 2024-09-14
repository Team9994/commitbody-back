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
@Transactional
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
    public void saveArticleFile(Article article, MultipartFile multipartFile) {
        String storedFilename = s3Service.uploadImage(multipartFile);
        String originalFilename = multipartFile.getOriginalFilename();
        FileType fileType = checkFileType(multipartFile);
        File file = File.of(originalFilename, storedFilename, fileType, article);
        fileRepository.save(file);
    }

    /**
     * 게시글의 파일을 수정합니다.
     * 게시글의 업로드한 파일과 동일하지 않을 경우에만 새롭게 저장합니다.
     * @param article 파일의 저장된 게시글
     * @param previousFileName  S3에 저장된 이전 파일명
     * @param multipartFile 파일
     */
    @Override
    public void updateArticleFile(Article article, String previousFileName, MultipartFile multipartFile) {
        if (!previousFileName.equals("")) {
            File file = fileRepository.findByArticleId(article.getId());
            FileType fileType = checkFileType(multipartFile);
            String storedFileName = s3Service.updateImage(multipartFile, previousFileName);
            String originalFilename = multipartFile.getOriginalFilename();
            if (!file.getOriginName().equals(originalFilename)) {
                file.update(originalFilename, storedFileName, fileType);
            }
        }else saveArticleFile(article,multipartFile);

    }


    /**
     * 파일의 유형을 검사합니다.
     * @param multipartFile
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
