package team9499.commitbody.domain.comment.article.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;

import java.util.List;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleCommentDto {

    private Long commentId;     // 댓글 ID
    private String content;     // 댓글 내용
    private String nickname;    // 작성자명
    private String profile;     // 사용자 프로필
    private String time;        // 작성 시간
    private Integer likeCount;    // 좋아요수
    private Integer replyCount; // 답글 수
    private boolean writer; //작성자

    private List<ArticleCommentDto> childComments;

    public static ArticleCommentDto of(Long commentId,String content,String nickname,String profile,String time,Integer likeCounter,boolean writer){
        return ArticleCommentDto.builder().commentId(commentId).content(content).profile(profile).nickname(nickname).time(time).likeCount(likeCounter).writer(writer).build();
    }

    public static ArticleCommentDto of(ArticleComment articleComment,String time, boolean writer,boolean reply){
        return ArticleCommentDto.builder().commentId(articleComment.getId()).content(articleComment.getContent()).profile(articleComment.getMember().getProfile())
                .nickname(articleComment.getMember().getNickname()).time(time).likeCount(articleComment.getLikeCount()).writer(writer).replyCount(reply ? articleComment.getChildComments().size() : null).build();
    }

    public static ArticleCommentDto of(ArticleComment articleComment, MemberDto memberDto, String time, boolean writer, boolean reply){
        return ArticleCommentDto.builder().commentId(articleComment.getId()).content(articleComment.getContent()).profile(memberDto.getProfile())
                .nickname(memberDto.getNickname()).time(time).likeCount(articleComment.getLikeCount()).writer(writer).replyCount(reply ?articleComment.getChildComments().size(): null ).build();
    }

}
