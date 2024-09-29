package team9499.commitbody.domain.block.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import team9499.commitbody.domain.block.servcice.ElsBlockMemberService;
import team9499.commitbody.domain.follow.service.FollowService;

@Component
@RequiredArgsConstructor
public class ElsBlockMemberHandler {

    private final ElsBlockMemberService blockMemberService;
    private final FollowService followService;

    @EventListener
    public void elsBlockMember(ElsBlockMemberEvent elsBlockMemberEvent){
        blockMemberService.blockMember(elsBlockMemberEvent.getBlockerId(), elsBlockMemberEvent.getBlockedId(),elsBlockMemberEvent.getStatus());
    }

    @EventListener
    public void cancelFollowBlockMember(CancelBlockMemberEvent cancelBlockMemberEvent){
        followService.cancelFollow(cancelBlockMemberEvent.getFollowerId(), cancelBlockMemberEvent.getFollowIngId(), cancelBlockMemberEvent.getStatus());
    }
}
