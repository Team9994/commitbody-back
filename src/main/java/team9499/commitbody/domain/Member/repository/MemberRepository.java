package team9499.commitbody.domain.Member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findBySocialIdAndLoginType(String socialId, LoginType loginType);

    Optional<Member> findByNickname(String nickname);
}
