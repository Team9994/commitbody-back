package team9499.commitbody.domain.follow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.follow.dto.FollowDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowResponse {

    private boolean hasNext;
    private List<FollowDto> follows;
}
