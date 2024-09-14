package team9499.commitbody.domain.file.service;

import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.file.domain.FileType;

public interface FileService {

    void saveArticleFile(Article article, MultipartFile file);

    void updateArticleFile(Article article, String previousFileName, MultipartFile file);

    FileType checkFileType(MultipartFile file);
}
