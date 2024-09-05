package team9499.commitbody.domain.Member.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.MemberDoc;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.Member.dto.response.MemberInfoResponse;
import team9499.commitbody.domain.Member.repository.MemberDocRepository;
import team9499.commitbody.domain.block.servcice.ElsBlockMemberService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberDocService {

    private final MemberDocRepository memberDocRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final ElsBlockMemberService elsBlockMemberService;

    private final String NICKNAME_FIELD = "nickname";
    private final String MEMBER_INDEX = "member_index";
    private final String ID ="id";

    /**
     * 회원가입시 회원 DOC에 닉네임을 비동기로 저장
     */
    @Async
    public void saveMemberDocAsync(MemberDoc memberDoc) {
        memberDocRepository.save(memberDoc);
    }

    /**
     * 닉네임을 통해 가입된 사용자 검색 - 엘라스틱 서치 사용
     * @param memberId  현재 로그인한 사용자 ID
     * @param nickname  검색할 사용자 닉네임
     * @param from  시작 페이지
     * @param size  페이지당 최대 크기
     * @return MemberInfoResponse 객체 반호나
     */
    public MemberInfoResponse findMemberForNickname(Long memberId, String nickname, int from, int size) {
        BoolQuery.Builder builder = new BoolQuery.Builder();    // bool 빌더 생성

        if (nickname != null) {     // 검색할 닉네임 존재시
            QueryStringQuery query = new QueryStringQuery.Builder()     
                    .query("*" + nickname + "*")    // 와일드 카드를 이용한 쿼리 생성
                    .fields(NICKNAME_FIELD)     // 닉네임 필드
                    .defaultOperator(Operator.And).build();     // and 연산자 사용
            builder.must(Query.of(q -> q.queryString(query)));
        }

        List<Long> blockerIds = elsBlockMemberService.getBlockerIds(memberId);
        blockerIds.add(memberId);

        // 현재 검색할 사용자와 차단한 사용자는 검색에서 제외해야한다.
        TermsQueryField termsQueryField = new TermsQueryField.Builder()
                .value(blockerIds.stream().map(FieldValue::of).toList()).build();

        builder.mustNot(Query.of(q -> q.terms(t -> t.field("_"+ID).terms(termsQueryField))));

        // 검색 요청 빌더 생성
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(MEMBER_INDEX)
                .query(Query.of(q -> q.bool(builder.build())))
                .size(size)
                .from(from).build();

        MemberInfoResponse memberInfoResponse = new MemberInfoResponse();       // 조회할 데이터를 담은 빈 객체 생성

        try {
            SearchResponse<Object> search = elasticsearchClient.search(searchRequest, Object.class);        // 검색 요청
            List<Hit<Object>> hits = search.hits().hits();      // 조회된 데이터를 꺼낸다.

            long value = search.hits().total().value();     // 검색된 총 데이터 수
            List<MemberDto> memberDtos = new ArrayList<>();

            // hits의 hit 리스트를 돌면서 사용자 id와 사용자 닉네임을 memberDtos 리스트에 담습니다.
            for (Hit<Object> hit : hits) {
                Map<String, Object> source = (Map<String, Object>) hit.source();
                memberDtos.add(MemberDto.createNickname(Long.valueOf(source.get(ID).toString()), source.get(NICKNAME_FIELD).toString(),source.get("profile").toString()));
            }

            memberInfoResponse.setTotalCount(value);
            memberInfoResponse.setMembers(memberDtos);

        }catch (Exception e){
            log.error("회원 검색중 오류 발생");
        }
        return memberInfoResponse;
    }
}
