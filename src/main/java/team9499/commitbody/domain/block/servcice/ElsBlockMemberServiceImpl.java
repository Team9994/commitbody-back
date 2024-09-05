package team9499.commitbody.domain.block.servcice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.block.domain.BlockMemberDoc;
import team9499.commitbody.domain.block.repository.ElsBlockMemberRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ElsBlockMemberServiceImpl implements ElsBlockMemberService{

    private final ElsBlockMemberRepository elsBlockMemberRepository;

    private String BLOCKED="blocked_";
    @Async
    public void blockMember(Long blockerId, Long blockedId, String status) {
        String id = getElsId(blockedId);

        BlockMemberDoc blockMemberDoc = elsBlockMemberRepository.findById(id)
                .orElse(new BlockMemberDoc(id, new ArrayList<>()));

        List<Long> blockedIds = blockMemberDoc.getBlockerId();

        if ("차단 성공".equals(status)) {
            if (!blockedIds.contains(blockerId)) {
                blockedIds.add(blockerId);
            }
        } else {
            blockedIds.remove(blockerId);
        }

        blockMemberDoc.setBlockerId(blockedIds);
        elsBlockMemberRepository.save(blockMemberDoc);
    }

    @Override
    public List<Long> getBlockerIds(Long blockedId) {
        String elsId = getElsId(blockedId);
        BlockMemberDoc blockMemberDoc = elsBlockMemberRepository.findById(elsId).orElse(null);
        if (blockMemberDoc!=null){
            return blockMemberDoc.getBlockerId();
        }
        return List.of();
    }

    private String getElsId(Long blockedId) {
        return BLOCKED + blockedId;
    }
}
