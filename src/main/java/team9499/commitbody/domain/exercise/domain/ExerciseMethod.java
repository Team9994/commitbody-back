package team9499.commitbody.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.exercise.domain.Exercise;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "exercise")
@Table(name = "exercise_method")
public class ExerciseMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_method")
    private Long id;

    @Column(name = "exercise_content", length = 2000)
    private String exerciseContent;

    @JoinColumn(name = "exercise_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Exercise exercise;

}
