package team9499.commitbody.global.authorization.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import team9499.commitbody.global.authorization.service.AuthorizationElsService;
import team9499.commitbody.global.redis.RedisService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WittDrawnMemberEventHandler {

    private final AuthorizationElsService authorizationElsService;
    private final RedisService redisService;
    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    /**
     * 비동기를 통한 엘라스틱 인덱스 필드값 변경및 배치 실행
     */
    @Async
    @EventListener
    public void deleteMemberEvent(DeleteMemberEvent deleteMemberEvent) throws Exception{
        authorizationElsService.updateDrawWriteUpdate(deleteMemberEvent.getMemberId(),deleteMemberEvent.getStatus());
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("memberId", deleteMemberEvent.getMemberId())
                .addString("action", deleteMemberEvent.getStatus())
                .addLocalDateTime("date", deleteMemberEvent.getDate()).toJobParameters();

        jobLauncher.run(jobRegistry.getJob("updateBatchJob"),jobParameters);
    }

    @Async
    @EventListener
    public void deleteRedisNickname(Long memberId){
        log.info("실행");
        redisService.deleteNicknameAllByMemberId(memberId);
    }
}
