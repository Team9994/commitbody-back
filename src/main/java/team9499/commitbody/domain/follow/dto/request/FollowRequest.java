package team9499.commitbody.domain.follow.dto.request;

import lombok.Data;
import team9499.commitbody.domain.follow.domain.FollowType;

@Data
public class FollowRequest {

    private Long followId;

    private FollowType type;
}
