package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
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
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;
import team9499.commitbody.domain.exercise.service.ElasticExerciseInterestService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ElasticExerciseServiceImplTest {

    @Mock private ElasticsearchClient client;
    @Mock private ElasticExerciseInterestService elasticExerciseInterestService;

    @InjectMocks private ElasticExerciseServiceImpl elasticExerciseService;

    @Test
    @DisplayName("엘락스틱을 통한 운동 검색")
    void searchExercise() throws Exception{
        SearchResponse<Object> response = mock(SearchResponse.class);

        List<Hit<Object>> mockHitList = new ArrayList<>();
        HitsMetadata<Object> hitsMetadata = mock(HitsMetadata.class);
        TotalHits totalHits = mock(TotalHits.class);
        Hit<Object> hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(Map.of(
                EXERCISE_ID,"1",
                EXERCISE_NAME,"등운동",
                EXERCISE_GIF,"이미지 주소",
                EXERCISE_TARGET,"등",
                EXERCISE_EQUIPMENT,"BAND",
                SOURCE,"default"));
        mockHitList.add(hit1);

        when(hitsMetadata.hits()).thenReturn(mockHitList);
        when(totalHits.value()).thenReturn((long) mockHitList.size());
        when(hitsMetadata.total()).thenReturn(totalHits);
        when(response.hits()).thenReturn(hitsMetadata);

        List<ExerciseDto> list = new ArrayList<>();
        list.add(ExerciseDto.of(1L,"등운동","이미지 주소","등","무게와 횟수","BAND","default",true));


        when(elasticExerciseInterestService.updateInterestFieldStatus(anyString(),any())).thenReturn(list);
        when(client.search(any(SearchRequest.class),any())).thenReturn(response);
        SearchExerciseResponse searchExerciseResponse = elasticExerciseService.searchExercise("등운동", null,null,0,10,null,"1","default");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client,times(1)).search(captor.capture(),any());
        SearchRequest value = captor.getValue();


        BoolQuery boolQuery = value.query().bool();

        List<Query> queries = new ArrayList<>();
        queries.add(new Query.Builder().bool(b -> b.mustNot(q -> q.exists(e -> e.field(MEMBER_ID)))).build());
        queries.add(new Query.Builder().term(t -> t.field(MEMBER_ID).value("1")).build());

        assertThat(searchExerciseResponse.getTotalCount()).isEqualTo(1L);
        assertThat(searchExerciseResponse.getExercise().size()).isEqualTo(1);
        assertThat(value.index()).contains(EXERCISE_INDEX);
        assertThat(boolQuery.filter().stream().map(Query::toString).toList()).contains(new Query.Builder().term(t -> t.field(SOURCE).value("default")).build().toString());
        assertThat(boolQuery.must().stream().map(Query::toString).toList()).contains(new Query.Builder().queryString(q -> q.query("*" + "등운동" + "*")
                .fields(EXERCISE_NAME)
                .defaultOperator(Operator.And)).build().toString());
        assertThat(boolQuery.should())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(queries);
    }

    @DisplayName("엘라스틱 운동 인덱스 - 업데이트")
    @Test
    void updateByElsExerciseIndex() throws Exception{
        CustomExerciseDto customExerciseDto = CustomExerciseDto.builder().exerciseName("등운동").gifUrl("이미지 주소").exerciseEquipment(ExerciseEquipment.BAND).exerciseTarget(ExerciseTarget.등).exerciseId(1L).memberId(1L).build();
        UpdateResponse<Object> response = mock(UpdateResponse.class);
        when(client.update(any(UpdateRequest.class),any())).thenReturn(response);

        elasticExerciseService.updateExercise(customExerciseDto,"default");
        ArgumentCaptor<UpdateRequest> captor = ArgumentCaptor.forClass(UpdateRequest.class);
        verify(client,times(1)).update(captor.capture(),any());


        UpdateRequest value = captor.getValue();
        assertThat(value.index()).isEqualTo(EXERCISE_INDEX);
        assertThat(value.id()).isEqualTo("default_1-1");
        assertThat(((Map<String, String>) value.doc()).values()).containsAll(List.of("이미지 주소","등운동","밴드","등"));
    }
    
    @DisplayName("엘라스틱 커스텀 운동 인덱스 - 제거")
    @Test
    void deleteByElsCustomExerciseIndex() throws Exception{
        DeleteResponse response = mock(DeleteResponse.class);
        when(client.delete(any(DeleteRequest.class))).thenReturn(response);

        elasticExerciseService.deleteExercise(1L,2L);

        ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(client,times(1)).delete(captor.capture());

        DeleteRequest value = captor.getValue();
        assertThat(value.index()).isEqualTo(EXERCISE_INDEX);
        assertThat(value.id()).isEqualTo("custom_1-2");
    }
    
    @DisplayName("사용자 탈퇴시 탈퇴 사용자 필드값을 업데이트")
    @Test
    void updateExerciseInterestWithDrawAsync() throws Exception {
        UpdateByQueryResponse response = mock(UpdateByQueryResponse.class);
        when(response.updated()).thenReturn(1L);
        when(client.updateByQuery(any(UpdateByQueryRequest.class))).thenReturn(response);

        elasticExerciseService.updateExerciseInterestWithDrawAsync(1L ,true);
        ArgumentCaptor<UpdateByQueryRequest> captor = ArgumentCaptor.forClass(UpdateByQueryRequest.class);
        verify(client,times(1)).updateByQuery(captor.capture());

        UpdateByQueryRequest value = captor.getValue();

        assertThat(value.index()).contains(INTEREST_INDEX);
        assertThat(value.query().toString()).isEqualTo(new Query.Builder().bool(b -> b.must(m -> m.term(t -> t.field(MEMBER_ID).value(1)))).build().toString());
        assertThat(value.script().inline().lang()).isEqualTo("painless");
        assertThat(value.script().inline().source()).isEqualTo("ctx._source.withDraw = params.writDraw");
        assertThat(value.script().inline().params().toString()).isEqualTo("{withDraw=true}");
    }

}