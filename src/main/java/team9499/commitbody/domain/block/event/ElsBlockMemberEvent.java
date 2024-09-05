package team9499.commitbody.domain.block.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ElsBlockMemberEvent {

    private Long blockerId;       //차단하는 사용자
    
    private Long blockedId;         //차단 당하는 사용자
    
    private String status;          // 차단 여부

}
