package team9499.commitbody.domain.block.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.block.domain.BlockMember;

@Repository
public interface BlockMemberRepository extends JpaRepository<BlockMember,Long>{

    BlockMember findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
