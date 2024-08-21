package team9499.commitbody.domain.comment.exercise.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ExerciseCommentDto {

    private Long exerciseCommentId;     // 댓글 Id (pk)

    private String nickName;    // 사용자 닉네임

    private String content;     // 댓글 내용

    private String commentedAt; // 1분전 하루전

    private boolean writer;     // 작성자일시 true, 아닐시 false

    private Integer likeCount;      // 좋아요 수

    private boolean likeStatus;     // 좋이요 상태
    
    public static ExerciseCommentDto of(Long exerciseCommentId,String nickName, String content, String commentedAt, boolean writer, Integer likeCount,boolean likeStatus){
        return new ExerciseCommentDto(exerciseCommentId,nickName,content,commentedAt,writer,likeCount,likeStatus);
    }

}
