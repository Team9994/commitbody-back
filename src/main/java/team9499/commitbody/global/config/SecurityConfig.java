package team9499.commitbody.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.global.security.CustomAccessDeniedHandler;
import team9499.commitbody.global.security.CustomAuthenticationEntryPoint;
import team9499.commitbody.global.security.filter.ExceptionFilter;
import team9499.commitbody.global.security.filter.JwtAuthenticationFilter;
import team9499.commitbody.global.utils.JwtUtils;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RedisService redisService;
    private final MemberRepository memberRepository;
    private final JwtUtils jwtUtils;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).disable());
        http
                .exceptionHandling((exception) -> exception.authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler()));

        http
                .addFilterBefore(jwtAuthenticationFilter(), BasicAuthenticationFilter.class)        //JWT 인증 필터
                .addFilterBefore(new ExceptionFilter(), UsernamePasswordAuthenticationFilter.class);        // 시큐리티단에서 예외 발생

        http.authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                        .requestMatchers("/api/v1/auth","/actuator/**","/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html","/api-docs/**",
                                "/api/v1/additional-info","/api/v1/scheduled/**"
                                ).permitAll()
                        .requestMatchers("/api/v1/**").hasAnyRole("USER"));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Arrays.asList("*"));        //모든 요청의 허용
        config.setAllowedMethods(Arrays.asList("HEAD","POST","GET","DELETE","PUT"));
        config.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private JwtAuthenticationFilter jwtAuthenticationFilter(){
        return new JwtAuthenticationFilter(redisService,memberRepository,jwtUtils);
    }
}
