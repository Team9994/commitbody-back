package team9499.commitbody.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.utils.CustomMapper;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService{

    private final RedisTemplate<String,Object> redisTemplate;
    private final MemberRepository memberRepository;
    private final String MEMBER_ID = "member_";
    private final String FCM = "fcm_";

    @Override
    public void setValue(String key, String value) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key,value);
    }

    @Override
    public void setValues(String key, String value, Duration duration) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key,value,duration);
    }

    @Override
    public String getValue(String key) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        if(values.get(key) == null) return "";
        return String.valueOf(values.get(key));
    }

    @Override
    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void setMember(Member member,Duration duration) {
        redisTemplate.opsForValue().set(MEMBER_ID+member.getId(),member,duration);
    }

    @Override
    public Optional<Member> getMemberDto(String key) {
        Object o = redisTemplate.opsForValue().get(getKey(key));
        if (o!=null){
            CustomMapper<Member> customMapper = new CustomMapper<>();
            return Optional.of(customMapper.to(o, Member.class));
        }else {
            Member member = memberRepository.findById(Long.valueOf(key)).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
            setMember(member,Duration.ofDays(7));
            return Optional.of(member);
        }

    }

    @Override
    public void updateMember(String key,Member member) {
        deleteValue(getKey(key));
        setMember(member,Duration.ofDays(30));
    }

    @Override
    public boolean nicknameLock(String key, String value, Duration duration) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value);
        if (result!=null && result){
            redisTemplate.expire(key,duration);
            return true;
        }
        return false;
    }

    @Override
    public void setFCM(String memberId, String token) {
        redisTemplate.opsForValue().setIfAbsent(FCM+memberId,token);
    }

    @Override
    public String getFCMToken(String key) {
        Object fcm = redisTemplate.opsForValue().get(FCM + key);
        if (fcm == null){
            return "";
        }
        return (String) fcm;
    }

    private String getKey(String key) {
        return MEMBER_ID + key;
    }
}
