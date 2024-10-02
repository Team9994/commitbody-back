package team9499.commitbody.domain.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.article.domain.Article;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, CustomArticleRepository {

    @Query(value = "select ra.*,f.stored_name, m.nickname ,m.profile, f.file_type " +
            "from (select a.* ,row_number() over (PARTITION by a.article_category order by a.like_count desc) as rn " +
            "from article a " +
            "left join block_member bm1 on bm1.blocked_id = a.member_id and bm1.blocker_id = :memberId " +
            "left join block_member bm2 on bm2.blocked_id = :memberId and bm2.blocker_id = a.article_id " +
            "where a.visibility ='전체 공개' and a.article_type = '정보 질문' " +
            "and (bm1.block_id IS NULL OR bm1.block_status = 0) and  (bm2.block_id IS NULL OR bm2.block_status = 0) " +
            "and a.created_at between date_format(now(), '%Y-%m-01') and last_day(now())) ra " +
            "join member m on m.member_id = ra.member_id " +
            "left join file f on f.article_id = ra.article_id " +
            "where ra.rn <= 2 and m.is_withdrawn = false",nativeQuery = true)
    List<Object[]> findByPopularArticle(@Param("memberId") Long memberId);

}
