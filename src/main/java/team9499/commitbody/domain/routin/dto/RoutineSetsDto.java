package team9499.commitbody.domain.routin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import team9499.commitbody.domain.routin.domain.RoutineSets;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class RoutineSetsDto {

    private Long setsId;

    private Integer kg;

    private Integer sets;

    private Integer times;

    public static RoutineSetsDto fromDto(RoutineSets routineSets){
        RoutineSetsDtoBuilder setsDtoBuilder = RoutineSetsDto.builder().setsId(routineSets.getId());

        Integer routineSetsSets = routineSets.getSets();
        Integer routineSetsKg = routineSets.getKg();
        Integer routineSetsTimes = routineSets.getTimes();

        if (routineSetsTimes!=null)setsDtoBuilder.times(routineSetsTimes);
        else if (routineSets!=null && routineSetsKg !=null) setsDtoBuilder.kg(routineSetsKg).sets(routineSetsSets);
        else setsDtoBuilder.sets(routineSetsSets);

        return setsDtoBuilder.build();
    }
}
