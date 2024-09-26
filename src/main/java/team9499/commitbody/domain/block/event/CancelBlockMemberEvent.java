package team9499.commitbody.domain.block.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CancelBlockMemberEvent {

    private Long followerId;

    private Long followIngId;

    private String status;
}
