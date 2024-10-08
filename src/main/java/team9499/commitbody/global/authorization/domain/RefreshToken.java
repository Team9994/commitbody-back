package team9499.commitbody.global.authorization.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.Member;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    @Column(name = "refresh_token")
    private String refreshToken;

    @OneToOne
    @JoinColumn(name = "member_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    private LocalDateTime expired;

    private RefreshToken( Member member, String refreshToken, LocalDateTime expired) {
        this.refreshToken = refreshToken;
        this.member = member;
        this.expired = expired;
    }

    public static RefreshToken of(Member member, String refreshToken,LocalDateTime expired){
        return new RefreshToken(member,refreshToken,expired);
    }
}
