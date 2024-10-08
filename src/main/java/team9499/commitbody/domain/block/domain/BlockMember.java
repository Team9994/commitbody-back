package team9499.commitbody.domain.block.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.global.utils.BaseTime;

@Entity
@Data
@Builder
@Table(name = "block_member", indexes ={
        @Index(name = "idx_blocker_id",columnList = "blocker_id"),
        @Index(name = "idx_blocked_id",columnList = "blocked_id")
})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@ToString(exclude = "{blocker,blocked}")
public class BlockMember extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long id;

    @JoinColumn(name = "blocker_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Member blocker;         // 차단한 사람

    @JoinColumn(name = "blocked_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Member blocked;         // 차단 당한 사람

    @Column(name = "block_status")
    private boolean blockStatus;    // 차단 상태

    public static BlockMember of(Member blocker,Member blocked){
        return BlockMember.builder().blocker(blocker).blocked(blocked).blockStatus(true).build();
    }

    public void updateStatus(boolean blockStatus){
        this.blockStatus = blockStatus;
    }
}
