package team9499.commitbody.domain.routin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.routin.dto.RoutineDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyRoutineResponse {

    private List<RoutineDto> routineDtos;
}
