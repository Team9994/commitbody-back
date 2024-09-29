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

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "exercise")
public class Exercise {

    @Id
    @Column(name = "exercise_id")
    private Long id;

    @Column(name = "exercise_name")
    private String exerciseName;

    @Column(name = "gif_url")
    private String gifUrl;

    @Column(name = "exercise_target")
    @Enumerated(EnumType.STRING)
    private ExerciseTarget exerciseTarget;

    @Column(name = "exercise_type")
    @Convert(converter = ExerciseTypeConverter.class)
    private ExerciseType exerciseType;

    @Column(name = "exercise_equipment")
    @Convert(converter = ExerciseEquipmentConverter.class)
    private ExerciseEquipment exerciseEquipment;

    private float mets;      // 운동 강도

    @OneToMany(fetch = FetchType.LAZY,mappedBy = "exercise")
    private List<ExerciseMethod> exerciseMethodList;

    public void updateGifUrl(String gifUrl){
        this.gifUrl = gifUrl;
    }
}
