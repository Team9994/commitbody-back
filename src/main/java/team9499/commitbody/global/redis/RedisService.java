package team9499.commitbody.global.redis;

import team9499.commitbody.domain.Member.domain.Member;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface RedisService {

    void setValue(String key, String value);

    void setValues(String key, String value, Duration duration);

    String getValue(String key);

    void deleteValue(String key,AuthType type);

    void setMember(Member member,Duration duration);

    Optional<Member> getMemberDto(String key);

    void updateMember(String key,Member member);

    boolean nicknameLock(String key, String value,Duration duration);

    void setFCM(String memberId, String token);

    String getFCMToken(String key);

    void setRecentSearchLog(String memberId, String title);

    List<Object> getRecentSearchLogs(String memberId);

    void deleteRecentSearchLog(String memberId, String title,String type);

    void setBlackListJwt(String jwtToken);

    boolean validBlackListJwt(String jwtToken);
}
