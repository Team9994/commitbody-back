package team9499.commitbody.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.exercise.domain.converter.ExerciseEquipmentConverter;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "custom_exercise")
public class CustomExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_ex_id")
    private Long id;

    @Column(name = "custom_ex_name")
    private String customExName;

    @Column(name = "custom_gif_url")
    private String customGifUrl;

    @Column(name = "exercise_target")
    @Enumerated(EnumType.STRING)
    private ExerciseTarget exerciseTarget;

    @Column(name = "exercise_equipment")
    @Convert(converter = ExerciseEquipmentConverter.class)
    private ExerciseEquipment exerciseEquipment;

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    public static CustomExercise save(String exerciseName,String gifUrl, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment,Member member){
        return CustomExercise.builder()
                .customExName(exerciseName).customGifUrl(gifUrl).exerciseTarget(exerciseTarget).exerciseEquipment(exerciseEquipment).member(member).build();
    }

    public void update(String exerciseName, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment,String updateUrl){
        this.customExName = exerciseName;
        this.customGifUrl= updateUrl;
        this.exerciseTarget = exerciseTarget;
        this.exerciseEquipment = exerciseEquipment;
    }
}
