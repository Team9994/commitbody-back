package team9499.commitbody.domain.block.servcice;

public interface BlockMemberService {

    String blockMember(Long blockerId, Long blockedId);
    Boolean checkBlock(Long blockerId, Long blockedId);
}
