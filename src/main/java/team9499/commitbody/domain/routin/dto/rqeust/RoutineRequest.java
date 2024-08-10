package team9499.commitbody.domain.routin.dto.rqeust;

import lombok.Data;

import java.util.List;

@Data
public class RoutineRequest {

    private String routineName;

    private List<Long> defaults;

    private List<Long> customs;
}
