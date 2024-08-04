package team9499.commitbody.global.redis;

import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.MemberDto;

import java.time.Duration;
import java.util.Optional;

public interface RedisService {

    void setValue(String key, String value);
    void setValues(String key, String value, Duration duration);
    String getValue(String key);
    void deleteValue(String key);

    void setMember(Member member);

    Optional<Member> getMemberDto(String key);

}
