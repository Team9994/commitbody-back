package team9499.commitbody.domain.block.servcice;


import java.util.List;

public interface ElsBlockMemberService {
    void blockMember(Long blockerId,Long blockedId,String status);

    List<Long> getBlockerIds(Long blockedId);

    List<Long> findBlockedIds(Long blockerId);
}
