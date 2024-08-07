package team9499.commitbody.domain.exercise.contorller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import team9499.commitbody.domain.exercise.service.SchedulingService;

@Slf4j
@Hidden
@RestController
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    @Hidden
    @PostMapping("/api/v1/scheduled")
    public void updateGifUrlSch(){
        schedulingService.updateGifUrl();
    }

    @Hidden
    @PostMapping("/api/v1/scheduled/elastic")
    public void updateElData(){
        schedulingService.updateElData();
    }

//    @Scheduled(cron = "0 01 03 * * ?")
//    public void scheduled(){
//        log.info("스케쥴링 실행");
//        updateGifUrlSch();
//    }

    @Scheduled(cron = "0 02 03 * * ?")
    public void scheduledElastic(){
        log.info("엘라스틱 데이터 업데이트 실행");
        updateElData();
    }

}
