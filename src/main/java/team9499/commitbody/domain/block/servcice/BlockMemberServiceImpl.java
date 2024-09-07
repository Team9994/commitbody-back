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

    /**
     * 마이페이지 접근시 차단 상태 유뮤 체크
     * 상대방이 현재 사용자를 차단했을경우에는 예외를 발생합니다.
     * @param blockerId 차단한 사용자
     * @param blockedId 차단된 사용자
     * @return 차단하지 않았을경우 false, 차단됬을 경우 true
     */
    @Override
    public Boolean checkBlock(Long blockerId, Long blockedId) {
        return blockMemberRepository.checkBlock(blockerId, blockedId)        // 상대방이 나를 차단했을경우
                .map(result -> {
                    if (result) {   //차단했다면 예외발생
                        throw new BlockException(ExceptionStatus.BAD_REQUEST, ExceptionType.BLOCK);
                    }
                    return false; // 사용자가 차단하지 않은 경우
                })
                .orElseGet(() -> {
                    boolean isBlocked = blockMemberRepository.checkBlock(blockedId, blockerId).orElse(false);   //내가 상대방을 차단했을경우
                    return isBlocked ? true : false; // 상대방을 차단했을경우 true 차단하지 않았을 경우 false
                });
    }
}
