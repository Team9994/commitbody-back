package team9499.commitbody.domain.article.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleType;
import team9499.commitbody.domain.article.domain.Visibility;
import team9499.commitbody.domain.follow.domain.Follow;
import team9499.commitbody.domain.follow.domain.FollowStatus;
import team9499.commitbody.domain.like.domain.ContentLike;
import team9499.commitbody.global.utils.TimeConverter;

import java.time.LocalDateTime;

@Data
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleDto {

    private Long articleId;     // 게시글 ID

    private Boolean postOwner;      // 게시글 작성자 체크   [ture : 작성자 , false : 타사용자]

    private FollowStatus followStatus; // 팔로우 상태

    private String title;       // 게시글 제목

    private String content;     //게시글 내용

    private ArticleCategory articleCategory;        // 게시글 타입

    private String time;        // 작성 시간

    private Integer likeCount;      // 좋아요 수

    private Boolean likeStatus;     // 좋아요 상태

    private Integer commentCount;  // 게시글 수

    private String imageUrl;        // 이미지 url

    private MemberDto member;       // 사용자

    @JsonIgnore
    private LocalDateTime localDateTime;

    @JsonIgnore
    private Visibility visibility;

    public static ArticleDto of(Long articleId, String imageUrl){
        return ArticleDto.builder().articleId(articleId).imageUrl(imageUrl).build();
    }

    public static ArticleDto of(Article article, String imageUrl){
        return ArticleDto.builder().articleId(article.getId()).title(article.getTitle()).articleCategory(article.getArticleCategory()).time(TimeConverter.converter(article.getCreatedAt())).likeCount(article.getLikeCount())
                .commentCount(article.getCommentCount()).imageUrl(imageUrl).build();
    }

    public static ArticleDto of(Article article,Member member, String imageUrl){
        MemberDto memberDTO = MemberDto.toMemberDTO(member);
        return ArticleDto.builder().articleId(article.getId()).title(article.getTitle()).content(article.getContent()).articleCategory(article.getArticleCategory()).time(TimeConverter.converter(article.getCreatedAt())).likeCount(article.getLikeCount())
                .commentCount(article.getCommentCount()).member(memberDTO).imageUrl(imageUrl).localDateTime(article.getCreatedAt()).visibility(article.getVisibility()).build();
    }

    public static ArticleDto of(Long loginMemberId,Article article, String imageUrl, Follow follow, ContentLike contentLike){
        Member member = article.getMember();
        ArticleDtoBuilder builder = ArticleDto.builder()
                .articleId(article.getId())
                .content(article.getContent())
                .time(TimeConverter.converter(article.getCreatedAt()))
                .postOwner(loginMemberId.equals(member.getId()))
                .imageUrl(imageUrl).followStatus(follow == null ? FollowStatus.CANCEL : follow.getStatus())
                .likeCount(article.getLikeCount())
                .likeStatus(contentLike != null && contentLike.isLikeStatus())
                .member(MemberDto.builder().memberId(member.getId()).nickname(member.getNickname()).profile(member.getProfile()).build());
        if (article.getArticleType().equals(ArticleType.INFO_QUESTION)){
            builder.title(article.getTitle()).articleCategory(article.getArticleCategory()).build();
        }
        return builder.build();
    }

    public static ArticleDto of(Long loginMemberId,Article article, String imageUrl, Member member,Follow follow){
        ArticleDtoBuilder builder = ArticleDto.builder();
        if (article.getArticleType().equals(ArticleType.EXERCISE)){
            builder.imageUrl(imageUrl).articleCategory(article.getArticleCategory()).articleId(article.getId()).postOwner(null);
        }else{
            MemberDto memberDto = MemberDto.builder().memberId(member.getId()).nickname(member.getNickname()).profile(member.getProfile()).build();
            builder.articleId(article.getId()).title(article.getTitle()).content(article.getContent()).articleCategory(article.getArticleCategory()).time(TimeConverter.converter(article.getCreatedAt())).likeCount(article.getLikeCount())
                    .commentCount(article.getCommentCount()).imageUrl(imageUrl).member(memberDto)
                    .postOwner(loginMemberId.equals(member.getId()))
                    .followStatus(follow == null ? null : follow.getStatus()).build();
        }
        return builder.build();
    }

    public static ArticleDto of(Long articleId,Long memberId ,ArticleCategory articleCategory, String content, String title, Integer likeCount, Integer commentCount,String time ,String imgUrl, String nickname,String profile){
        MemberDto memberDto = MemberDto.builder().memberId(memberId).nickname(nickname).profile(profile).build();
        return ArticleDto.builder().articleId(articleId).articleCategory(articleCategory).content(content).title(title).likeCount(likeCount).commentCount(commentCount).time(time).imageUrl(imgUrl).member(memberDto).build();
    }

}
