package team9499.commitbody.domain.block.servcice.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.block.domain.BlockMemberDoc;
import team9499.commitbody.domain.block.repository.ElsBlockMemberRepository;
import team9499.commitbody.domain.block.servcice.ElsBlockMemberService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static team9499.commitbody.global.constants.Delimiter.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;

@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ElsBlockMemberServiceImpl implements ElsBlockMemberService {

    private final ElsBlockMemberRepository elsBlockMemberRepository;
    private final ElasticsearchClient elasticsearchClient;

    private final String BLOCKED="blocked_";
    private final static String BLOCK_KR = "차단 성공";

    /**
     * 차단 성공/해제시 BlockMember 의 차단한 사용자 필드의 차단한 사용자 Id 를 삽입/삭제 하는 메서드
     * @param blockerId 차단한 사용자
     * @param blockedId 차단당한 사용자
     * @param status   [차단 성공, 차단 해제]
     */
    @Async
    public void blockMember(Long blockerId, Long blockedId, String status) {
        BlockMemberDoc blockMemberDoc = getBlockMemberDoc(blockedId);
        handleBlockMember(blockMemberDoc, getBlockedIds(blockerId, status, blockMemberDoc));
    }

    /**
     * 차단당한 사용자의 차단한 사용자의 ID를 조회
     * @param blockedId 차단당한 사용자의 ID
     * @return 차단한 사용자가 있을시 List 반환 없을시 빈 List 반환
     */
    @Override
    public List<Long> getBlockerIds(Long blockedId) {
        BlockMemberDoc blockMemberDoc = getBlockerMemberDoc(blockedId);
        return handlerBlockerIds(blockMemberDoc);
    }

    /**
     * 차단한 사용자가 차단된 사용자의 ID를 조회하는 메서드
     * @param blockerId 차단한 사용자 ID
     * @return  차단된 사용자 ID를 List 반환
     */
    @Override
    public List<Long> findBlockedIds(Long blockerId) {
        SearchRequest searchRequest = createSearchRequest(blockerId);
        return handlerFindBlockedIds(searchRequest);
    }

    private BlockMemberDoc getBlockMemberDoc(Long blockedId) {
        String id = getElsId(blockedId);
        return elsBlockMemberRepository.findById(id)
                .orElse(new BlockMemberDoc(id, new ArrayList<>()));
    }

    private void handleBlockMember(BlockMemberDoc blockMemberDoc, List<Long> blockedIds) {
        blockMemberDoc.setBlockerId(blockedIds);
        elsBlockMemberRepository.save(blockMemberDoc);
    }

    private List<Long> getBlockedIds(Long blockerId, String status, BlockMemberDoc blockMemberDoc) {
        List<Long> blockedIds = blockMemberDoc.getBlockerId();
        if (BLOCK_KR.equals(status)) {
            blockedIds.add(blockerId); // 중복 추가를 방지하도록 수정
            return blockedIds.stream().distinct().toList();
        }
        blockedIds.remove(blockerId);
        return blockedIds;
    }

    private BlockMemberDoc getBlockerMemberDoc(Long blockedId) {
        return elsBlockMemberRepository.findById(getElsId(blockedId)).orElse(null);
    }

    private static List<Long> handlerBlockerIds(BlockMemberDoc blockMemberDoc) {
        if (blockMemberDoc !=null){
            return blockMemberDoc.getBlockerId();
        }
        return List.of();
    }

    private static SearchRequest createSearchRequest(Long blockerId) {
        return new SearchRequest.Builder()
                .index(BLOCK_MEMBER_INDEX)
                .query(q -> q.term(t -> t.field(BLOCKER_ID).value(blockerId))).build();
    }

    private List<Long> handlerFindBlockedIds(SearchRequest searchRequest) {
        List<Long> ids = new ArrayList<>();
        try {
            SearchResponse<Object> search = elasticsearchClient.search(searchRequest, Object.class);
            extractIdsFromHits(search, ids);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ids;
    }

    private void extractIdsFromHits(SearchResponse<Object> search, List<Long> ids) {
        for (Hit<Object> hit : search.hits().hits()) {
            ids.add(Long.parseLong(hit.id().replace(BLOCKED, STRING_EMPTY)));
        }
    }

    private String getElsId(Long blockedId) {
        return BLOCKED + blockedId;
    }
}
