package team9499.commitbody.global.authorization.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.UpdateByQueryRequest;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.service.MemberDocService;
import team9499.commitbody.domain.article.service.ElsArticleService;
import team9499.commitbody.domain.exercise.service.ElasticExerciseService;

import java.io.IOException;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class AuthorizationElsService {

    private final ElasticsearchClient elasticsearchClient;
    private final ElsArticleService elsArticleService;
    private final ElasticExerciseService elasticExerciseService;
    private final MemberDocService memberDocService;
    private final String MEMBER_FIELD = "memberId";


    /**
     * 비동기를 통한 사용자와 관련된 데이터의 상태를 변경
     * @param memberId  변경할 사용자 ID
     * @param status    TURE : 회원 탈퇴 , FALSE : 재가입
     */
    @Async
    public void updateDrawWriteUpdate(Long memberId, String status){
        boolean type = status.equals("탈퇴");
        elsArticleService.updateArticleWithDrawAsync(memberId,type);        // 게시글
        elsArticleService.updateArticleLikeAndCommentCountAsync(memberId,type);     // 게시글 좋아요 댓글수 업데이트
        elasticExerciseService.updateExerciseInterestWithDrawAsync(memberId,type);  // 관심 운동
        memberDocService.updateMemberWithDrawAsync(memberId,type); // 사용자
    }

    @Async
    public void deleteElsByMemberId(Long memberId){
        
        // 운동 필드 삭제
        DeleteByQueryRequest deleteByExerciseIndex = new DeleteByQueryRequest.Builder().index("exercise_index")
                .query(getQuery(memberId)).build();

        // 관심 운동 필드 삭제
        DeleteByQueryRequest deleteInterestIndex = new DeleteByQueryRequest.Builder().index("exercise_interest_index")
                .query(getQuery(memberId)).build();

        // 사용자 필드 삭제
        DeleteByQueryRequest deleteByMemberIndex = new DeleteByQueryRequest.Builder().index("member_index")
                .query(getQuery(memberId)).build();

        // 게시글 필드 삭제
        DeleteByQueryRequest deleteByArticleRequest = new DeleteByQueryRequest.Builder().index("article_index")
                .query(getQuery(memberId)).build();

        // 차단된 사용자 필드 삭제
        DeleteByQueryRequest deleteByBlockRequest = new DeleteByQueryRequest.Builder().index("block_member_index")
                .query(Query.of(q -> q.term(t -> t.field("id").value("blocked_"+memberId))))
                .build();

        // 차단한 사용자 목록에서 사용자 제거
        UpdateByQueryRequest request = new UpdateByQueryRequest.Builder()
                .index("block_member_index")  // 인덱스 이름 설정
                .query(q -> q
                        .term(t -> t
                                .field("blockerId")  // 업데이트할 조건 필드
                                .value(memberId)           // 해당 필드의 값
                        )
                )
                .script(s -> s.inline(i -> i.source("ctx._source.blockerId.removeIf(id -> id == params.id)")
                        .params("id", JsonData.of(memberId)))
                )
                .build();

        try {
            DeleteByQueryResponse deleteByExerciseResponse = elasticsearchClient.deleteByQuery(deleteByExerciseIndex);
            DeleteByQueryResponse deleteByInterestResponse= elasticsearchClient.deleteByQuery(deleteInterestIndex);
            DeleteByQueryResponse deleteByMemberResponse= elasticsearchClient.deleteByQuery(deleteByMemberIndex);
            DeleteByQueryResponse deleteByArticleResponse= elasticsearchClient.deleteByQuery(deleteByArticleRequest);
            DeleteByQueryResponse deleteByBlockResponse= elasticsearchClient.deleteByQuery(deleteByBlockRequest);
            UpdateByQueryResponse updateByQueryResponse = elasticsearchClient.updateByQuery(request);
            log.info("{}번 회원 탈퇴시 엘라스틱 데이터 삭제 [운동 : {}, 관심 운동 : {}, 게시글 : {}, 사용자 : {} , 차단 목록 업데이트 : {}, 차단 사용자 삭제 : {}]",
                    memberId,deleteByExerciseResponse.deleted(),deleteByInterestResponse.deleted(),deleteByArticleResponse.deleted(),
                    deleteByMemberResponse.deleted(),updateByQueryResponse.updated(),deleteByBlockResponse.deleted());

        } catch (IOException e) {
            log.error("회원 탈퇴시 엘라스틱 데이터 삭제 도중 오류 발생 : {}",e.getMessage());
        }
    }

    private Query getQuery(Long memberId) {
        return Query.of(q -> q.term(t -> t.field(MEMBER_FIELD).value(memberId)));
    }
}
