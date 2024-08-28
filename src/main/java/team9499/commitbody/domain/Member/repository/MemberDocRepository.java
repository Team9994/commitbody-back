package team9499.commitbody.domain.Member.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.Member.domain.MemberDoc;

@Repository
public interface MemberDocRepository extends ElasticsearchRepository<MemberDoc,String> {
}
