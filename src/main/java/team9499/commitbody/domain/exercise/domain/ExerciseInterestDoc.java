package team9499.commitbody.domain.exercise.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "exercise_interest_index" , writeTypeHint = WriteTypeHint.FALSE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExerciseInterestDoc {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long memberId;

    @Field(type = FieldType.Long)
    private Long exerciseId;

    @Field(type = FieldType.Boolean, name = "status")       //관심운동 상태
    private Boolean status;
}