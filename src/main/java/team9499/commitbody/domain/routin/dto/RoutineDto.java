package team9499.commitbody.domain.routin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.routin.domain.Routine;

import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoutineDto {

    private Long routineId;             // 루틴 아이디

    private String routineName;         // 루틴 명

    private Set<String> targets;        // 운동 부위 명칭

    private List<Object> exercises;     // 루틴의 운동 목록

    public static RoutineDto of(Routine routine){
        return RoutineDto.builder().routineId(routine.getId()).routineName(routine.getRoutineName()).build();
    }

    public void setData(Set<String> targets,List<Object> exercises){
        this.targets = targets;
        this.exercises = exercises;
    }
}
