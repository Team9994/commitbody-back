package team9499.commitbody.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.dto.MemberDto;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.utils.CustomMapper;

import java.time.Duration;
import java.util.*;

import static team9499.commitbody.global.Exception.ExceptionStatus.BAD_REQUEST;
import static team9499.commitbody.global.Exception.ExceptionType.DUPLICATE_NICKNAME;
import static team9499.commitbody.global.constants.Delimiter.*;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService{

    private final RedisTemplate<String,Object> redisTemplate;
    private final MemberRepository memberRepository;
    private static final String MEMBER_ID = "member_";
    private static final String MEMBER_PATTERN = "member_*";
    private static final String NICKNAME_PATTERN = "nickname_*";
    private static final String NICKNAME = "nickname_";
    private static final String FCM = "fcm_";
    private static final String SEARCH = "search_";
    private static final String ALL = "all";
    private static final String BLACK_LIST = "blackList";

    @Override
    public void deleteValue(String key, AuthType type) {
        String redisKey = STRING_EMPTY;
        switch (type){
            case CERTIFICATION -> redisKey = getKey(key);
            case SEARCH -> redisKey = getSearchKey(key);
            case FCM -> redisKey = FCM+key;
        }
        redisTemplate.delete(redisKey);
    }

    @Override
    public void setMember(Member member,Duration duration) {
        redisTemplate.opsForValue().set(MEMBER_ID+member.getId(),member,duration);
    }

    @Override
    public Optional<Member> getMemberDto(String key) {
        Object memberOb = redisTemplate.opsForValue().get(getKey(key));
        if (memberOb!=null){
            return Optional.of(getMemberCustomMapper().to(memberOb, Member.class));
        }
        Member member = memberRepository.findById(Long.valueOf(key))
                .filter(member1 -> !member1.isWithdrawn())
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
        setMember(member,Duration.ofDays(7));
        return Optional.of(member);
    }


    @Override
    public void getMemberAllNickname(String nickname) {
        Set<String> nicknames = getMemberNicknameByKey();
        List<String> members = new ArrayList<>();
        addNicknamesToMembers(nicknames, members);
        validateUniqueNickname(nickname, members);

    }

    @Override
    public void existNickname(String nickname,Long memberId){
        Set<String> nicknameKey = getNicknameKey();
        if (isDuplicateNicknameKey(nickname, memberId, nicknameKey))
            return;
        if (isDuplicateNicknameValue(nickname, nicknameKey))
            throw new InvalidUsageException(BAD_REQUEST, DUPLICATE_NICKNAME);
    }

    @Override
    public void updateMember(String key,Member member) {
        deleteValue(getKey(key),AuthType.CERTIFICATION);
        setMember(member,Duration.ofDays(30));
    }

    @Override
    public void setFCM(String memberId, String token) {
        redisTemplate.opsForValue().setIfAbsent(FCM+memberId,token);
    }

    @Override
    public String getFCMToken(String key) {
        Object fcm = redisTemplate.opsForValue().get(FCM + key);
        if (fcm == null){
            return STRING_EMPTY;
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
            if (type.equals(ALL)) deleteValue(memberId,AuthType.SEARCH);
        }else
            redisTemplate.opsForList().remove(searchKey,0,title);
    }

    /**
     * 로그아웃시 현재 사용한 JWT토큰을 블랙리스트의 1시간동안 추가합니다.
     * @param jwtToken 현재 사용한 JWT 토큰
     */
    @Override
    public void setBlackListJwt(String jwtToken) {
        redisTemplate.opsForValue().set(jwtToken,BLACK_LIST,Duration.ofHours(1));
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

    @Override
    public void setNickname(MemberDto memberDto) {
        redisTemplate.opsForValue()
                .set(NICKNAME+memberDto.getMemberId()+HYPHEN+memberDto.getNickname(),memberDto.getNickname(),
                        Duration.ofHours(1));
    }

    @Override
    public void deleteNicknameAllByMemberId(Long memberId) {
        Set<String> keys = redisTemplate.keys(NICKNAME + memberId+HYPHEN+STAR);
        redisTemplate.delete(keys);
    }

    private Set<String> getMemberNicknameByKey() {
        return redisTemplate.keys(MEMBER_PATTERN);
    }

    private void addNicknamesToMembers(Set<String> nicknames, List<String> members) {
        for (String nicknameStr : nicknames) {
            Object ob = redisTemplate.opsForValue().get(nicknameStr);
            members.add(getMemberCustomMapper().to(ob,Member.class).getNickname());
        }
    }

    private static void validateUniqueNickname(String nickname, List<String> members) {
        members.stream()
                .filter(Objects::nonNull)
                .forEach(s -> {
                    if (s.equals(nickname)) {
                        throw new InvalidUsageException(BAD_REQUEST, DUPLICATE_NICKNAME);
                    }
                });
    }

    private Set<String> getNicknameKey() {
        Set<String> nicknameKey = redisTemplate.keys(NICKNAME_PATTERN);
        if (nicknameKey == null) {
            nicknameKey = Collections.emptySet();
        }
        return nicknameKey;
    }

    private static boolean isDuplicateNicknameKey(String nickname, Long memberId, Set<String> nicknameKey) {
        return nicknameKey.stream()
                .anyMatch(key -> {
                    String[] parts = key.split(HYPHEN, 3);
                    return parts.length == 3
                            && parts[1].equals(memberId.toString())
                            && parts[2].equals(nickname);
                });
    }

    private boolean isDuplicateNicknameValue(String nickname, Set<String> nicknameKey) {
        return nicknameKey.stream()
                .map(key -> (String) redisTemplate.opsForValue().get(key))
                .anyMatch(value -> nickname.equals(value));
    }


    private String getKey(String key) {
        return MEMBER_ID + key;
    }

    private String getSearchKey(String memberId) {
        return SEARCH + memberId;
    }

    private static CustomMapper<Member> getMemberCustomMapper() {
        return new CustomMapper<>();
    }

}
