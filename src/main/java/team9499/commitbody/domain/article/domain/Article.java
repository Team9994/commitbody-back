package team9499.commitbody.domain.article.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.converter.ArticleCategoryConverter;
import team9499.commitbody.domain.article.domain.converter.ArticleTypeConverter;
import team9499.commitbody.domain.article.domain.converter.VisibilityConverter;
import team9499.commitbody.global.utils.BaseTime;

@Entity
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(indexes = {
        @Index(name = "idx_article_exercise", columnList = "article_type,created_at,member_id"),
})
@NoArgsConstructor
public class Article extends BaseTime {

    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_id")
    private Long id;                    // 아이디(pk)

    private String title;               // 제목

    @Column(length = 5000)
    private String content;             // 내용

    private Integer likeCount;          // 좋아요 수

    private Integer commentCount;       // 댓글 수

    @Convert(converter = ArticleTypeConverter.class)
    private ArticleType articleType;            //EXERCISE("운동 인증"), INFO_QUESTION("정보 질문")

    @Convert(converter = ArticleCategoryConverter.class)
    private ArticleCategory articleCategory;        //  information("정보")

    @Convert(converter = VisibilityConverter.class)
    private Visibility visibility;          //  PUBLIC("전체 공개"), FOLLOWERS_ONLY("팔로워만 공개"), PRIVATE("비공개")

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;


    public static Article of(String title, String content, ArticleType articleType,ArticleCategory articleCategory,Visibility visibility,Member member){
        ArticleBuilder articleBuilder = Article.builder().title(title).content(content).likeCount(0).commentCount(0).visibility(visibility).member(member);
        if (articleType.equals(ArticleType.EXERCISE)){
            articleBuilder.articleType(ArticleType.EXERCISE);
        }else if (articleType.equals(ArticleType.INFO_QUESTION)){
            articleBuilder.articleType(ArticleType.INFO_QUESTION).articleCategory(articleCategory);
        }
        return articleBuilder.build();
    }
    public void updateCommentCount(Integer count){
        this.commentCount = count;
    }

    public void updateLikeCount(Integer count){
        this.likeCount = count;
    }

    public void update(String title, String content, ArticleType articleType,ArticleCategory articleCategory,Visibility visibility){
        this.title = title;
        this.content = content;
        this.articleType = articleType;
        if (articleType.equals(ArticleType.INFO_QUESTION)){
            this.articleCategory = articleCategory;
        }
        this.visibility = visibility;

    }
}
