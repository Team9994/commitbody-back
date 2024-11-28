package team9499.commitbody.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RecordDetailsDto {
    private int detailsVolume;
    private int detailsSets;        // 상세 운동별 총 세트
    private int detailsReps;        // 상세 운도별 총 횟수
    private int detailsTime;        // 상세 운동별 수행 시간
    private int maxRm;              // 최대 1rm
    private int weightCount;
    private int maxReps;
    private boolean weightValid;
    private boolean timesValid;

    public static RecordDetailsDto init(){
        return new RecordDetailsDto(0,0,0,0,0,0,0,false,false);
    }

    public void detailSetsPlus(){
        this.detailsSets +=1;
    }

    public void setWeightAndReps(int weight, int reps){
        this.detailsVolume += weight*reps;
        this.detailsReps += reps;
        this.weightValid = true;
        this.maxRm +=  Math.round((float) weight * (float) (1 + 0.03333 * reps));
        this.weightCount++;
    }

    public void setTimes(int times){
        this.detailsTime += times;
        this.timesValid = true;
    }

    public void setReps(int maxReps, int reps){
        this.maxReps = Math.max(maxReps, reps);
        this.detailsReps +=reps;
    }
}
