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

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_ex_id")
    private Long id;

    private String customExName;

    private String customGifUrl;

    @Enumerated(EnumType.STRING)
    private ExerciseTarget exerciseTarget;

    @Convert(converter = ExerciseEquipmentConverter.class)
    private ExerciseEquipment exerciseEquipment;

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    public CustomExercise save(String exerciseName,String gifUrl, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment,Member member){
        return CustomExercise.builder()
                .customExName(exerciseName).customGifUrl(gifUrl).exerciseTarget(exerciseTarget).exerciseEquipment(exerciseEquipment).member(member).build();
    }
}
