package team9499.commitbody.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.converter.ExerciseEquipmentConverter;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.domain.converter.ExerciseTypeConverter;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Exercise {

    @Id
    @Column(name = "exercise_id")
    private Long id;

    private String exerciseName;

    private String gifUrl;

    @Enumerated(EnumType.STRING)
    private ExerciseTarget exerciseTarget;

    @Convert(converter = ExerciseTypeConverter.class)
    private ExerciseType exerciseType;

    @Convert(converter = ExerciseEquipmentConverter.class)
    private ExerciseEquipment exerciseEquipment;

    private float mets;      // 운동 강도

    public void updateGifUrl(String gifUrl){
        this.gifUrl = gifUrl;
    }
}
