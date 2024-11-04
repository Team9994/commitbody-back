package team9499.commitbody.domain.block.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.block.domain.BlockMemberDoc;

@Repository
public interface ElsBlockMemberRepository extends ElasticsearchRepository<BlockMemberDoc,String> {
}
