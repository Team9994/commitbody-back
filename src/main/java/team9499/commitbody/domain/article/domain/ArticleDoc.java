package team9499.commitbody.domain.article.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import team9499.commitbody.domain.article.dto.ArticleDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@Document(indexName = "article_index",writeTypeHint = WriteTypeHint.FALSE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class ArticleDoc {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword,name = "category")
    private ArticleCategory articleCategory;

    @Field(type = FieldType.Text,name = "title")
    private String title;

    @Field(type = FieldType.Text,name = "content")
    private String content;

    @Field(type = FieldType.Text,name = "img_url")
    private String imgUrl;

    @Field(type = FieldType.Long, name = "memberId")
    private Long memberId;

    @Field(type = FieldType.Text,name = "writer")
    private String writer;

    @Field(type = FieldType.Integer,name = "like_count")
    private Integer likeCount;

    @Field(type = FieldType.Integer,name = "comment_count")
    private Integer commentCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private String time;

    @Field(type = FieldType.Keyword,name = "visibility")
    private Visibility visibility;

    @Field(type = FieldType.Boolean, name = "withDraw")
    private Boolean withDraw;

    public static ArticleDoc of(ArticleDto articleDto){
        return ArticleDoc.builder().id(articleDto.getArticleId())
                .articleCategory(articleDto.getArticleCategory())
                .title(articleDto.getTitle())
                .content(articleDto.getContent())
                .likeCount(articleDto.getLikeCount())
                .commentCount(articleDto.getCommentCount())
                .writer(articleDto.getMember().getNickname())
                .memberId(articleDto.getMember().getMemberId())
                .time(converterTime(articleDto.getLocalDateTime()))
                .imgUrl(articleDto.getImageUrl())
                .visibility(articleDto.getVisibility())
                .withDraw(false)
                .build();
    }
    static String converterTime(LocalDateTime localDateTime){
        return localDateTime.format(DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss"));
    }
}
