package team9499.commitbody.global.batch.mapper;

import org.springframework.jdbc.core.RowMapper;
import team9499.commitbody.domain.Member.domain.Member;

import java.sql.ResultSet;
import java.sql.SQLException;


public class CustomMemberRowMapper implements RowMapper<Member> {

    @Override
    public Member mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Member.builder()
                .id(rs.getLong("member_id")).build();
    }
}
