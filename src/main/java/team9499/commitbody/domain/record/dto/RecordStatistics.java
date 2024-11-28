package team9499.commitbody.domain.record.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordStatistics {

    private int totalVolume;
    private int totalSets;
    private int exerciseSize;
    private int calories;

    public static RecordStatistics init(){
        return new RecordStatistics(0,0,0,0);
    }

    public void exerciseSizePlus(){
        this.exerciseSize++;
    }


    public void totalSetPlus(){
        this.totalSets +=1;
    }

    public void totalVolumePlus(int weight){
        this.totalVolume+=weight;
    }

    public void caloriesPlus(int calories){
        this.calories += calories;
    }

}
