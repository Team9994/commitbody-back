package team9499.commitbody.domain.Member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.dto.MemberDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberInfoResponse {

    private Long totalCount;

    private List<MemberDto> members;
}
