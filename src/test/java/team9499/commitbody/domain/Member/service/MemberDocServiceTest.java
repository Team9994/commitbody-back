package team9499.commitbody.domain.Member.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.InlineScript;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.Member.dto.response.MemberInfoResponse;
import team9499.commitbody.domain.block.servcice.ElsBlockMemberService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class MemberDocServiceTest {

    @Mock private ElasticsearchClient client;
    @Mock private ElsBlockMemberService blockMemberService;

    @InjectMocks private MemberDocService memberDocService;


    @DisplayName("프로필 비동기 업데이트 ")
    @Test
    void updateElsMemberProfileAsync() throws Exception{
        // given
        UpdateResponse<Object> response = mock(UpdateResponse.class);
        when(client.update(any(UpdateRequest.class),any())).thenReturn(response);

        // when
        memberDocService.updateMemberDocAsync("1","변경한 닉네임","default.png");

        // then
        ArgumentCaptor<UpdateRequest> captor = ArgumentCaptor.forClass(UpdateRequest.class);
        verify(client, times(1)).update(captor.capture(), any());
        UpdateRequest value = captor.getValue();

        assertThat(value.id()).isEqualTo("1");
        assertThat((Map) value.doc()).isEqualTo(Map.of("profile","default.png","nickname","변경한 닉네임"));
    }

    @DisplayName("닉네임을 통한 사용자 검색")
    @Test
    void searchMemberByNickname() throws Exception{
        Long memberId = 1L;
        List<Long> blockedIds = List.of(2L, 3L);
        List<Long> blockerIds = List.of(4L, 5L);

        List<Hit<Object>> mockHitList = new ArrayList<>();
        SearchResponse<Object> response = mock(SearchResponse.class);
        HitsMetadata<Object> hitsMetadata = mock(HitsMetadata.class);
        TotalHits totalHits = mock(TotalHits.class);

        Hit<Object> hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(Map.of("memberId", "7", "nickname", "닉네임7", "profile", "default2.png"));
        mockHitList.add(hit1);

        when(hitsMetadata.hits()).thenReturn(mockHitList);
        when(totalHits.value()).thenReturn((long) mockHitList.size());
        when(hitsMetadata.total()).thenReturn(totalHits);
        when(response.hits()).thenReturn(hitsMetadata);

        when(blockMemberService.getBlockerIds(eq(memberId))).thenReturn(blockerIds);
        when(blockMemberService.findBlockedIds(eq(memberId))).thenReturn(blockedIds);
        when(client.search(any(SearchRequest.class), eq(Object.class))).thenReturn(response);

        // When
        MemberInfoResponse memberInfoResponse = memberDocService.findMemberForNickname(1L, "닉네", 0, 10);

        verify(client,times(1)).search(any(SearchRequest.class),any());

        assertThat(memberInfoResponse.getMembers().get(0).getNickname()).isEqualTo("닉네임7");
        assertThat(memberInfoResponse.getMembers().size()).isEqualTo(1);
    }


    @DisplayName("사용자 탈퇴시 필드 업데이트")
    @Test
    void updateMemberWithDrawAsync() throws Exception {
        // given
        UpdateByQueryResponse mockResponse = mock(UpdateByQueryResponse.class);
        when(mockResponse.updated()).thenReturn(1L);
        when(client.updateByQuery(any(UpdateByQueryRequest.class))).thenReturn(mockResponse);

        // When
        memberDocService.updateMemberWithDrawAsync(1L, true);

        // then
        ArgumentCaptor<UpdateByQueryRequest> captor = ArgumentCaptor.forClass(UpdateByQueryRequest.class);
        verify(client, times(1)).updateByQuery(captor.capture());
        UpdateByQueryRequest value = captor.getValue();

        assertThat(value.index()).contains("member_index");
        assertThat(Objects.requireNonNull(value.query()).toString()).isEqualTo(new Query.Builder().bool(b -> b.must(m -> m.term(t -> t.field("memberId").value(1)))).build().toString());
        InlineScript inlineScript = Objects.requireNonNull(value.script()).inline();
        assertThat(inlineScript.lang()).isEqualTo("painless");
        assertThat(inlineScript.source()).isEqualTo("ctx._source.withDraw = params.writDraw");
        assertThat(inlineScript.params().toString()).isEqualTo("{writDraw=true}");
    }


}