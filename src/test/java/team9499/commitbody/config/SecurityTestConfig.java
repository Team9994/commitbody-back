package team9499.commitbody.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.global.redis.RedisServiceImpl;
import team9499.commitbody.global.security.CustomAccessDeniedHandler;
import team9499.commitbody.global.security.CustomAuthenticationEntryPoint;
import team9499.commitbody.global.utils.JwtUtils;

@TestConfiguration
@Import({CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
public class SecurityTestConfig {

    @Bean
    public RedisService redisService(){
        return Mockito.mock(RedisServiceImpl.class);
    }

    @Bean
    public JwtUtils jwtUtils(){
        return Mockito.mock(JwtUtils.class);
    }

    @Bean
    public MemberRepository memberRepository(){
        return Mockito.mock(MemberRepository.class);
    }
}
