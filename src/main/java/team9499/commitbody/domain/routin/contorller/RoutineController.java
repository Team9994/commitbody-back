package team9499.commitbody.domain.routin.contorller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.routin.dto.rqeust.RoutineRequest;
import team9499.commitbody.domain.routin.service.RoutineService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RoutineController {

    private final RoutineService routineService;

    @PostMapping("/routine")
    public void saveRoutine(@RequestBody RoutineRequest maps,
                            @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        routineService.saveRoutine(memberId,maps.getDefaults(),maps.getCustoms(),maps.getRoutineName());
    }

}
