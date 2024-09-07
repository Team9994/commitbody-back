package team9499.commitbody.domain.block.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static team9499.commitbody.domain.block.domain.QBlockMember.*;

@Repository
@RequiredArgsConstructor
public class CustomBlockMemberRepositoryImpl implements CustomBlockMemberRepository{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Boolean> checkBlock(Long blockerId, Long blockedId) {
        Boolean status = jpaQueryFactory.select(blockMember.blockStatus)
                .from(blockMember)
                .where(blockMember.blocker.id.eq(blockerId).and(blockMember.blocked.id.eq(blockedId))).fetchOne();
        return Optional.ofNullable(status);
    }
}
