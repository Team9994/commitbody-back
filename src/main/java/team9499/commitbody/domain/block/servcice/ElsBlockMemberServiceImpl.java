package team9499.commitbody.domain.block.servcice;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.block.domain.BlockMemberDoc;
import team9499.commitbody.domain.block.repository.ElsBlockMemberRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ElsBlockMemberServiceImpl implements ElsBlockMemberService{

    private final ElsBlockMemberRepository elsBlockMemberRepository;
    private final ElasticsearchClient elasticsearchClient;

    private String BLOCKED="blocked_";

    /**
     * 차단 성공/해제시 BlockMember 의 차단한 사용자 필드의 차단한 사용자 Id 를 삽입/삭제 하는 메서드
     * @param blockerId 차단한 사용자
     * @param blockedId 차단당한 사용자
     * @param status   [차단 성공, 차단 해제]
     */
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

    /**
     * 차단당한 사용자의 차단한 사용자의 ID를 조회
     * @param blockedId 차단당한 사용자의 ID
     * @return 차단한 사용자가 있을시 List 반환 없을시 빈 List 반환
     */
    @Override
    public List<Long> getBlockerIds(Long blockedId) {
        String elsId = getElsId(blockedId);
        BlockMemberDoc blockMemberDoc = elsBlockMemberRepository.findById(elsId).orElse(null);
        if (blockMemberDoc!=null){
            return blockMemberDoc.getBlockerId();
        }
        return List.of();
    }

    /**
     * 차단한 사용자가 차단된 사용자의 ID를 조회하는 메서드
     * @param blockerId 차단한 사용자 ID
     * @return  차단된 사용자 ID를 List 반환
     */
    @Override
    public List<Long> findBlockedIds(Long blockerId) {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("block_member_index")
                .query(q -> q.term(t -> t.field("blockerId").value(blockerId))).build();

        List<Long> ids = new ArrayList<>();
        try {
            SearchResponse<Object> search = elasticsearchClient.search(searchRequest, Object.class);
            for (Hit<Object> hit : search.hits().hits()) {
                 ids.add(Long.parseLong(hit.id().replace(BLOCKED,"")));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ids;
    }

    private String getElsId(Long blockedId) {
        return BLOCKED + blockedId;
    }
}
