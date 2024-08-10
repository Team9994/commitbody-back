package team9499.commitbody.domain.routin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoutineDto {

    private Long routineId;             // 루틴 아이디

    private String routineName;         // 루틴 명

    private Set<String> targets;        // 운동 부위 명칭

    private List<Object> exercises;     // 루틴의 운동 목록
}
