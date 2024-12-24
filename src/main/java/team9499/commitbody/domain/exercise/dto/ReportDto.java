package team9499.commitbody.domain.exercise.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class ReportDto {

    private Integer max1Rm;
    private Integer maxRep;
    private Integer maxTime;
    private Integer totalVolume;
    private Integer totalRep;
    private Integer totalTime;
    private Integer weekVolume;
    private Integer weekRep;
    private Integer weekTime;
    private List<WeekReport> weekReports;


    public static ReportDtoBuilder weightOf(@Nullable Integer oneRm, @Nullable Integer totalVolume){
        return ReportDto.builder().max1Rm(oneRm).totalVolume(totalVolume);
    }

    public static ReportDtoBuilder repOf(@Nullable Integer maxRep, @Nullable Integer totalRep){
        return ReportDto.builder().maxRep(maxRep).totalRep(totalRep);
    }

    public static ReportDtoBuilder timeOf(@Nullable Integer maxTime, @Nullable Integer totalTime){
        return ReportDto.builder().maxTime(maxTime).totalTime(totalTime);
    }

}
