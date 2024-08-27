package team9499.commitbody.domain.follow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowResponse<T> {

    private boolean hasNext;
    private List<T> follows;
}
