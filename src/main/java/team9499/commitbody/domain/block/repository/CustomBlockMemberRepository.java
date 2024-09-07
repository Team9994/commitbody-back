package team9499.commitbody.domain.block.repository;


import java.util.Optional;

public interface CustomBlockMemberRepository {

    Optional<Boolean> checkBlock(Long blockerId, Long blockedId);
}
