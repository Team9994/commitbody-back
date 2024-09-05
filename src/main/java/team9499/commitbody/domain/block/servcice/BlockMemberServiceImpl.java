package team9499.commitbody.domain.block.servcice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.block.domain.BlockMember;
import team9499.commitbody.domain.block.repository.BlockMemberRepository;
import team9499.commitbody.global.Exception.BlockException;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.redis.RedisService;

@Service
@Transactional
@RequiredArgsConstructor
public class BlockMemberServiceImpl implements BlockMemberService{

    private final BlockMemberRepository blockMemberRepository;
    private final RedisService redisService;

    private final String BLOCK ="차단 성공";
    private final String NONBLOCK ="차단 해제";

    /**
     * 사용자 차단/해제 메서드
     * @param blockerId 차단하는 사용자
     * @param blockedId 차당된 사용자
     * @return  "차단 성공", "차단 해제" 반환
     */
    @Override
    public String blockMember(Long blockerId, Long blockedId) {
        Member blockerMember = redisService.getMemberDto(String.valueOf(blockerId)).get();      // 차단할 사용자
        Member blockedMember = redisService.getMemberDto(String.valueOf(blockedId)).get();      // 차단 당한 사용자
        String status ="";
        BlockMember blockMember = blockMemberRepository.findByBlockerIdAndBlockedId(blockerId, blockedId);

        // 이전에 차단했던 기록이 존재시
        if (blockMember!=null){
            boolean blockStatus = blockMember.isBlockStatus();
            if (blockStatus) {    // 차단된 상태라면 차단 해제
                blockMember.updateStatus(false);
                status = NONBLOCK;
            }
            else {
                blockMember.updateStatus(true);    //차단 해제 상태라면 차단
                status = BLOCK;
            }
        }else {     // 새로운 차단존재시
            blockMemberRepository.save(BlockMember.of(blockerMember, blockedMember));   // 차단된 상태로 저장
            status = BLOCK;
        }

        return status;
    }

}
