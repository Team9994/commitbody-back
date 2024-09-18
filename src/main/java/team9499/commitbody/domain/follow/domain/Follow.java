package team9499.commitbody.domain.follow.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.Member.domain.Member;

@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Data
@ToString(exclude = {"follower", "following"})
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)) // follower를 위한 컬럼
    private Member follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)) // following을 위한 컬럼
    private Member following;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",length = 20)
    private FollowStatus status;

    public static Follow create(Member follower, Member following,FollowType type){
        if (type.equals(FollowType.FOLLOWING)){
            return Follow.builder().follower(follower).following(following).status(FollowStatus.FOLLOWING).build();
        }else{
            return Follow.builder().follower(follower).following(following).status(FollowStatus.REQUEST).build();
        }
    }

    public void updateFollowStatus(FollowStatus followStatus){
        this.status = followStatus;
    }
}
