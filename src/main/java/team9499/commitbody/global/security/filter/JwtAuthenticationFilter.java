package team9499.commitbody.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.JwtTokenException;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.global.utils.JwtUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import static team9499.commitbody.global.constants.Delimiter.*;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private final RedisService redisService;
    private final MemberRepository memberRepository;
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(AUTHORIZATION);
        if (validHeaderToken(request, response, filterChain, header)) return;
        String jwtToken = header.replace(BEARER, STRING_EMPTY);
        validBlackListToken(jwtToken);
        setAuthenticationContext(jwtUtils.accessTokenValid(jwtToken));
        filterChain.doFilter(request,response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        return request.getServletPath()
                .equals("/api/v1/auth");
    }

    private static boolean validHeaderToken(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String header) throws IOException, ServletException {
        if(header == null|| !header.startsWith(BEARER)){
            filterChain.doFilter(request, response);
            return true;
        }
        return false;
    }

    private void validBlackListToken(String jwtToken) {
        boolean validBlackListJwt = redisService.validBlackListJwt(jwtToken);
        if (validBlackListJwt) throw new JwtTokenException(ExceptionStatus.FORBIDDEN,ExceptionType.LOGIN_REQUIRED);
    }

    private void setAuthenticationContext(String accessTokenValid) {
        Member member = getOptionalMember(accessTokenValid);
        PrincipalDetails principalDetails = new PrincipalDetails(member);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Member getOptionalMember(String accessTokenValid) {
        Optional<Member> optionalMember = redisService.getMemberDto(accessTokenValid);
        if (optionalMember.isEmpty()){
            Member member = getMember(accessTokenValid);
            redisService.setMember(member, Duration.ofHours(2));
            optionalMember = Optional.of(member);
        }
        return optionalMember.get();
    }

    private Member getMember(String accessTokenValid) {
        return memberRepository.findById(Long.parseLong(accessTokenValid))
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
    }



}
