package team9499.commitbody.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.file.domain.File;

@Repository
public interface FileRepository extends JpaRepository<File,Long> {

    File findByArticleId(Long articleId);
}
