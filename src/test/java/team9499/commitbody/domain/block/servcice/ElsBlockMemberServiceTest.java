package team9499.commitbody.domain.block.servcice;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.block.domain.BlockMemberDoc;
import team9499.commitbody.domain.block.repository.ElsBlockMemberRepository;
import team9499.commitbody.domain.block.servcice.impl.ElsBlockMemberServiceImpl;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ElsBlockMemberServiceTest {

    @Mock private ElsBlockMemberRepository blockMemberRepository;
    @Mock private ElasticsearchClient elasticsearchClient;

    @InjectMocks private ElsBlockMemberServiceImpl blockMemberService;

    private Long blockerId = 1L;
    private Long blockedId = 2L;

    @DisplayName("차단 성공시 List에 차단한 사용자 추가")
    @Test
    void blockMember(){

        BlockMemberDoc blockMemberDoc = BlockMemberDoc.builder().id("2").blockerId(new ArrayList<>()).build();

        when(blockMemberRepository.findById(eq("blocked_2"))).thenReturn(Optional.of(blockMemberDoc));
        when(blockMemberRepository.save(any())).thenReturn(blockMemberDoc);
        
        blockMemberService.blockMember(blockerId,blockedId,"차단 성공");

        assertThat(blockMemberDoc.getBlockerId()).contains(blockerId);
    }
    
    @DisplayName("차단 해제시 차단한 사용자 제거")
    @Test
    void UnBlockRemove(){
        List<Long> blockedIds = new ArrayList<>();
        blockedIds.add(1L);
        BlockMemberDoc blockMemberDoc = BlockMemberDoc.builder().id("2").blockerId(blockedIds).build();

        when(blockMemberRepository.findById(eq("blocked_2"))).thenReturn(Optional.of(blockMemberDoc));
        when(blockMemberRepository.save(any())).thenReturn(blockMemberDoc);

        blockMemberService.blockMember(blockerId,blockedId,"차단 해제");

        assertThat(blockMemberDoc.getBlockerId()).doesNotContain(blockerId);
    }

    @DisplayName("차단당한 사용자를 차단한 사용자 ID 값 조회")
    @Test
    void blockedByBlockerIds(){
        List<Long> blockedIds = new ArrayList<>();
        blockedIds.add(1L);
        blockedIds.add(3L);
        blockedIds.add(4L);
        BlockMemberDoc blockMemberDoc = BlockMemberDoc.builder().id("2").blockerId(blockedIds).build();

        when(blockMemberRepository.findById("blocked_2")).thenReturn(Optional.of(blockMemberDoc));

        List<Long> blockerIds = blockMemberService.getBlockerIds(blockedId);

        assertThat(blockerIds.size()).isEqualTo(3);
        assertThat(blockerIds).containsAll(List.of(1L,3L,4L));
    }
    
    
    @DisplayName("차단한 사용자가 차단된 사용자의 ID값을 조회")
    @Test
    void blockerByBlockedIds() throws IOException {

        SearchResponse<Object> searchResponse = mock(SearchResponse.class);

        List<Hit<Object>> mockHitList = new ArrayList<>();

        Hit<Object> mockHit1 = mock(Hit.class);
        Hit<Object> mockHit2 = mock(Hit.class);

        when(mockHit1.id()).thenReturn("blocked_2");
        when(mockHit2.id()).thenReturn("blocked_3");

        mockHitList.add(mockHit1);
        mockHitList.add(mockHit2);

        HitsMetadata hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.hits()).thenReturn(mockHitList);
        when(searchResponse.hits()).thenReturn(hitsMetadata);


        when(elasticsearchClient.search(any(SearchRequest.class),any())).thenReturn(searchResponse);

        List<Long> blockedIds = blockMemberService.findBlockedIds(blockerId);

        assertThat(blockedIds.size()).isEqualTo(2);
        assertThat(blockedIds).containsAll(List.of(2L,3L));

        verify(elasticsearchClient,times(1)).search(any(SearchRequest.class),any());
    }

}