package team9499.commitbody.domain.comment.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.comment.exercise.dto.ExerciseCommentDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseCommentResponse {

    private boolean hasNext;
    private List<ExerciseCommentDto> commentList;
}
