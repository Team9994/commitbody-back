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

    @Field(type = FieldType.Boolean,name = "favorites")             // 관심운동
    private Boolean favorites;


    public ExerciseDoc customExercise(CustomExercise customExercise,String gifUrl){
        return ExerciseDoc.builder()
                .id("custom_"+customExercise.getId())
                .exerciseId(customExercise.getId()).exerciseName(customExercise.getCustomExName()).gifUrl(gifUrl)
                .exerciseType(null).exerciseEquipment(customExercise.getExerciseEquipment().getKoreanName()).memberId(String.valueOf(customExercise.getMember().getId()))
                .source("custom").favorites(false).build();
    }


}
