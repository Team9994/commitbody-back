package team9499.commitbody.domain.block.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import team9499.commitbody.domain.block.servcice.ElsBlockMemberService;

@Component
@RequiredArgsConstructor
public class ElsBlockMemberHandler {

    private final ElsBlockMemberService blockMemberService;

    @EventListener
    public void elsBlockMember(ElsBlockMemberEvent elsBlockMemberEvent){
        blockMemberService.blockMember(elsBlockMemberEvent.getBlockerId(), elsBlockMemberEvent.getBlockedId(),elsBlockMemberEvent.getStatus());
    }
}
