package team9499.commitbody.mock;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;

import java.util.List;

public class CustomContextHolder implements WithSecurityContextFactory<MockUser> {

    @Override
    public SecurityContext createSecurityContext(MockUser annotation) {
        String loginId = annotation.loginId();
        Long id = annotation.id();
        Member member = Member.builder().id(id).nickname("테스트 닉네임").isWithdrawn(annotation.isWithDrawn()).socialId(loginId).loginType(LoginType.KAKAO).build();
        PrincipalDetails principalDetails = new PrincipalDetails(member);
        Authentication token = new UsernamePasswordAuthenticationToken(principalDetails, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(token);
        return context;
    }
}
