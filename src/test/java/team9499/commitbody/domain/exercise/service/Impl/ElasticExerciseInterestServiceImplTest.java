package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ElasticExerciseInterestServiceImplTest {

    @Mock
    private ElasticsearchClient client;

    @InjectMocks
    private ElasticExerciseInterestServiceImpl exerciseInterestService;

    @DisplayName("관심 운동 검색")
    @Test
    void searchFavoriteExercise() throws Exception {
        SearchResponse<Object> interestResponse = mock(SearchResponse.class);
        List<Hit<Object>> interestHitList = new ArrayList<>();
        HitsMetadata<Object> interestMetadata = mock(HitsMetadata.class);
        Hit<Object> interestHit = mock(Hit.class);

        when(interestHit.source()).thenReturn(Map.of(
                "id", "default_6-4"));
        interestHitList.add(interestHit);
        when(interestMetadata.hits()).thenReturn(interestHitList);
        when(interestResponse.hits()).thenReturn(interestMetadata);

        SearchResponse<Object> searchResponse = mock(SearchResponse.class);
        List<Hit<Object>> searchHitList = new ArrayList<>();
        HitsMetadata<Object> searchMetadata = mock(HitsMetadata.class);
        TotalHits searchTotalHit = mock(TotalHits.class);
        Hit<Object> searchHit = mock(Hit.class);

        when(searchHit.source()).thenReturn(Map.of(
                EXERCISE_ID, "1",
                EXERCISE_NAME, "운동이름",
                EXERCISE_GIF, "이미지 주소",
                EXERCISE_TARGET, "등",
                EXERCISE_EQUIPMENT, "BAND",
                SOURCE, "default"
        ));
        searchHitList.add(searchHit);
        when(searchMetadata.hits()).thenReturn(searchHitList);
        when(searchTotalHit.value()).thenReturn((long) searchHitList.size());
        when(searchMetadata.total()).thenReturn(searchTotalHit);
        when(searchResponse.hits()).thenReturn(searchMetadata);

        when(client.search(any(SearchRequest.class), any()))
                .thenReturn(interestResponse)
                .thenReturn(searchResponse);

         exerciseInterestService.searchFavoriteExercise("1", new BoolQuery.Builder(), 10, 0,
                new ArrayList<>()
        );

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client, times(2)).search(captor.capture(), any());

        List<SearchRequest> allValues = captor.getAllValues();

        assertThat(allValues.get(0).index()).contains("exercise_interest_index");
        assertThat(allValues.get(1).index()).contains("exercise_index");
    }

}