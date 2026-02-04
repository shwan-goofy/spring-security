package org.example.hsspringsecurity.week4.v14methodsecurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V14: 메소드 레벨 보안 설정
 * 
 * WEEK 4 학습 목표:
 * - @EnableMethodSecurity로 메소드 레벨 보안 활성화
 * - @PreAuthorize, @PostAuthorize 어노테이션 사용
 * - SpEL 표현식으로 세밀한 권한 제어
 * - URL 레벨과 메소드 레벨 보안의 차이 이해
 * 
 * @EnableMethodSecurity:
 * - Spring Security 6.0+에서 권장하는 메소드 보안 활성화 방법
 * - 이전 버전의 @EnableGlobalMethodSecurity 대체
 * - AOP 프록시 기반으로 동작
 * 
 * SecurityFilterChain 특징:
 * - @Order(14): V13 다음 순서
 * - securityMatcher("/v14/method/**", "/login", "/logout"): 경로 매칭
 * - URL 레벨에서는 authenticated()만 체크
 * - 실제 세밀한 권한 체크는 Service 메소드 레벨에서 수행
 * 
 * 필터 체인 (기본 구조):
 * 1. SecurityContextHolderFilter (순서 3)
 * 2. HeaderWriterFilter (순서 4)
 * 3. CsrfFilter (순서 6) ← csrf().disable()로 제거 가능
 * 4. LogoutFilter (순서 7)
 * 5. UsernamePasswordAuthenticationFilter (순서 9)
 * 6. BasicAuthenticationFilter (순서 12)
 * 7. RequestCacheAwareFilter (순서 14)
 * 8. AnonymousAuthenticationFilter (순서 17)
 * 9. ExceptionTranslationFilter (순서 20)
 * 10. AuthorizationFilter (순서 21) ← URL 레벨 권한 체크
 * 
 * 메소드 보안 동작 원리:
 * 1. @EnableMethodSecurity가 AOP 프록시 생성
 * 2. Service 메소드 호출 시 프록시가 먼저 실행
 * 3. @PreAuthorize 어노테이션의 SpEL 표현식 평가
 * 4. 조건 만족 시 실제 메소드 실행
 * 5. @PostAuthorize의 경우 메소드 실행 후 반환 값 기반 평가
 * 6. 조건 불만족 시 AccessDeniedException 발생 (403 Forbidden)
 * 
 * AOP 프록시 주의사항:
 * - 같은 클래스 내부에서 메소드를 직접 호출하면 프록시 우회
 * - 예: this.methodA()는 보안 어노테이션 무시됨
 * - 해결: 다른 Bean에서 호출하거나 self-injection 사용
 * 
 * 심층 방어(Defense in Depth) 전략:
 * - 1차 방어선: URL 레벨 (SecurityFilterChain)
 * - 2차 방어선: 메소드 레벨 (Service 메소드)
 * - URL 설정이 잘못되어도 메소드 레벨에서 보호
 * - 비즈니스 로직과 가까운 곳에서 최종 방어
 * 
 * 테스트 방법:
 * 1. ADMIN 전용:
 *    curl -u admin@example.com:admin123 http://localhost:8080/v14/method/admin-only
 *    → 200 OK
 *    
 *    curl -u user@example.com:user123 http://localhost:8080/v14/method/admin-only
 *    → 403 Forbidden (메소드 레벨에서 거부)
 * 
 * 2. 파라미터 체크:
 *    curl -u user@example.com:user123 \
 *      http://localhost:8080/v14/method/owner-only/user@example.com
 *    → 200 OK
 *    
 *    curl -u user@example.com:user123 \
 *      http://localhost:8080/v14/method/owner-only/other@example.com
 *    → 403 Forbidden
 */
@Configuration
@EnableMethodSecurity  // 메소드 레벨 보안 활성화 (Spring Security 6.0+)
public class MethodSecurityConfig {

    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService customUserDetailsService;

    @Bean
    @Order(14)
    public SecurityFilterChain methodSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v14/method/**", "/login", "/logout")
                
                // CSRF 비활성화 (테스트 편의)
                .csrf(csrf -> csrf.disable())
                
                // URL 기반 접근 제어
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트
                        .requestMatchers("/v14/method/public", "/v14/method/security-levels").permitAll()
                        
                        // 나머지는 인증만 필요 (역할 체크는 메소드 레벨에서)
                        // 여기서는 authenticated()만 체크하고
                        // 실제 세밀한 권한 체크는 Service의 @PreAuthorize에서 수행
                        .anyRequest().authenticated()
                )
                
                // Form Login 활성화
                .formLogin(withDefaults())
                
                // HTTP Basic 인증 활성화
                .httpBasic(withDefaults())
                
                // CustomUserDetailsService 사용
                .userDetailsService(customUserDetailsService);

        return http.build();
    }
}

