package team9499.commitbody.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.Data;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.exercise.domain.converter.ExerciseEquipmentConverter;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;

@Data
@Entity
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

}
