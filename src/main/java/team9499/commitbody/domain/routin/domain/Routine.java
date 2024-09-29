package team9499.commitbody.domain.routin.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.Member;

import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "routine")
public class Routine {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    @Column(name = "routine_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "routine_name")
    private String routineName;

    @OneToMany(mappedBy = "routine",cascade = CascadeType.REMOVE)
    private List<RoutineDetails> list;

    public static Routine create(Member member, String routineName){
        return Routine.builder().member(member).routineName(routineName).build();
    }

    public void updateRoutineName(String routineName){
        this.routineName = routineName;
    }

}
