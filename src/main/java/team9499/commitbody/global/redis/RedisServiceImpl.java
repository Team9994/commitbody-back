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
import java.util.List;
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
    public void deleteValue(String key, AuthType type) {
        String redisKey = "";
        switch (type){
            case CERTIFICATION -> redisKey = getKey(key);
            case SEARCH -> redisKey = getSearchKey(key);
            case FCM -> redisKey = "fcm_"+key;
        }
        redisTemplate.delete(redisKey);
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
        deleteValue(getKey(key),AuthType.CERTIFICATION);
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

    /**
     * 검색기록을 저장 - 최대 검색 기록은 30개를 저장가능하며 30개 이상일시 저장하면 마지막 검색기록은 삭제후 최신 검색 기록을 저장,
     * 기존 검색 기록이 존재한다면 해당 내역을 삭제후 맨 위에 저장
     * @param memberId  사용자 ID
     * @param title 검색 기록
     */
    @Override
    public void setRecentSearchLog(String memberId, String title) {
        String key = getSearchKey(memberId);

        // 리스트의 현재 크기와 범위 가져오기
        Long size = redisTemplate.opsForList().size(key);
        List<Object> range = redisTemplate.opsForList().range(key, 0, -1);

        // 제목이 리스트에 존재하면 제거
        if (range != null && range.contains(title)) {
            redisTemplate.opsForList().remove(key, 0, title);
        }

        // 크기가 30 이상이면 가장 오래된 항목 제거
        if (size != null && size >= 30) {
            redisTemplate.opsForList().rightPop(key);
        }

        // 제목을 리스트의 맨 앞에 추가
        redisTemplate.opsForList().leftPush(key, title);
    }

    /**
     * 사용자 검색 기록 조회
     * @param memberId  사용자 ID
     * @return  조회된 10개의 검색기록을 List 반환
     */
    @Override
    public List<Object> getRecentSearchLogs(String memberId) {
        String searchKey = getSearchKey(memberId);

        return redisTemplate.opsForList().range(searchKey, 0, 9);
    }

    /**
     * 검색 기록 삭제
     * @param memberId 사용자 ID
     * @param title 삭제할 제목
     * @param type  all 타입시 전체 데이터 삭제
     */
    @Override
    public void deleteRecentSearchLog(String memberId, String title,String type) {
        String searchKey = getSearchKey(memberId);
        if (type!=null){
            if (type.equals("all")) deleteValue(memberId,AuthType.SEARCH);
        }else
            redisTemplate.opsForList().remove(searchKey,0,title);
    }

    /**
     * 로그아웃시 현재 사용한 JWT토큰을 블랙리스트의 1시간동안 추가합니다.
     * @param jwtToken 현재 사용한 JWT 토큰
     */
    @Override
    public void setBlackListJwt(String jwtToken) {
        redisTemplate.opsForValue().set(jwtToken,"blackList",Duration.ofHours(1));
    }

    /**
     * 현재 블랙리스트의 토큰이 저장되어있는지 확인
     * @param jwtToken  현재 사용한 JWT 토큰
     * @return  존재시 TRUE, 미존재시 FALSE
     */
    @Override
    public boolean validBlackListJwt(String jwtToken) {
        return redisTemplate.hasKey(jwtToken);
    }

    private String getKey(String key) {
        return MEMBER_ID + key;
    }

    private String getSearchKey(String memberId) {
        return "search_" + memberId;
    }
}
