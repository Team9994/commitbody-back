package team9499.commitbody.domain.block.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import team9499.commitbody.domain.block.domain.BlockMemberDoc;

public interface ElsBlockMemberRepository extends ElasticsearchRepository<BlockMemberDoc,String> {
}
