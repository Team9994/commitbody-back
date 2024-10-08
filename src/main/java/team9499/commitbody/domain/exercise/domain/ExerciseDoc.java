package team9499.commitbody.domain.exercise.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;

@Data
@Builder
@Document(indexName = "exercise_index" , writeTypeHint = WriteTypeHint.FALSE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class ExerciseDoc {

    @Id
    private String id;

    @Field(type = FieldType.Long, name = "exerciseId")
    private Long exerciseId;                              // 운동 id

    @Field(type = FieldType.Text, name = "exerciseName")        // 운동명
    private String exerciseName;

    @Field(type = FieldType.Text, name = "gifUrl")              // url
    private String gifUrl;

    @Field(type = FieldType.Text, name = "exerciseTarget")      // 운동부위
    private String exerciseTarget;

    @Field(type = FieldType.Text, name = "exerciseType")        // 운동 기록 타입
    private String exerciseType;

    @Field(type = FieldType.Text, name = "exerciseEquipment")       // 운동 장비 방법
    private String exerciseEquipment;

    @Field(type = FieldType.Text, name = "memberId")                // 사용자 ID
    private String memberId;

    @Field(type = FieldType.Text, name = "source")                  // custom , default
    private String source;

    @Field(type = FieldType.Boolean,name = "interest")             // 관심운동
    private Boolean interest;


    public static ExerciseDoc customExercise(CustomExerciseDto customExerciseDto){
        return ExerciseDoc.builder()
                .id("custom_"+customExerciseDto.getExerciseId()+"-"+customExerciseDto.getMemberId())
                .exerciseId(customExerciseDto.getExerciseId())
                .exerciseName(customExerciseDto.getExerciseName())
                .gifUrl(customExerciseDto.getGifUrl())
                .exerciseType(null).exerciseEquipment(customExerciseDto.getExerciseEquipment().getKoreanName())
                .exerciseTarget(customExerciseDto.getExerciseTarget().name())
                .memberId(String.valueOf(customExerciseDto.getMemberId()))
                .source("custom").interest(false).build();
    }


}
