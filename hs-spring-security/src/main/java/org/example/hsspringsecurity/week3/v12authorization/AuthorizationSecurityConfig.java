package org.example.hsspringsecurity.week3.v12authorization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V12: 역할 기반 권한 부여(Authorization) 보안 설정
 * 
 * WEEK 3 학습 목표:
 * - 인증(Authentication)과 인가(Authorization)의 차이 이해
 * - 역할(Role) 기반 접근 제어 구현
 * - hasRole(), hasAnyRole(), hasAuthority() 메소드 활용
 * - 403 Forbidden vs 401 Unauthorized 구분
 * 
 * SecurityFilterChain 특징:
 * - @Order(12): V11 다음 순서
 * - securityMatcher("/v12/authorization/**", "/login", "/logout"): 경로 매칭
 * - authorizeHttpRequests(): URL 기반 세밀한 권한 제어
 * - hasRole("USER"): ROLE_USER 권한 필요 (접두사 자동 추가)
 * - hasRole("ADMIN"): ROLE_ADMIN 권한 필요
 * - hasAnyRole("USER", "ADMIN"): 둘 중 하나만 있으면 허용
 * - authenticated(): 인증만 되면 역할 상관없이 허용
 * - permitAll(): 인증 불필요
 * 
 * 필터 체인 (기본 구조, CSRF 활성화):
 * 1. SecurityContextHolderFilter (순서 3)
 * 2. HeaderWriterFilter (순서 4)
 * 3. CsrfFilter (순서 6) ← 기본 활성화
 * 4. LogoutFilter (순서 7)
 * 5. UsernamePasswordAuthenticationFilter (순서 9)
 * 6. BasicAuthenticationFilter (순서 12)
 * 7. RequestCacheAwareFilter (순서 14)
 * 8. AnonymousAuthenticationFilter (순서 17)
 * 9. ExceptionTranslationFilter (순서 20) ← 403/401 예외 처리
 * 10. AuthorizationFilter (순서 21) ← 여기서 권한 검사 수행
 * 
 * 인증 vs 인가:
 * - 인증(Authentication): "당신은 누구인가?" (신원 확인)
 *   → 실패 시 401 Unauthorized
 *   → UsernamePasswordAuthenticationFilter에서 처리
 * 
 * - 인가(Authorization): "당신은 무엇을 할 수 있는가?" (권한 확인)
 *   → 실패 시 403 Forbidden
 *   → AuthorizationFilter에서 처리
 * 
 * 역할(Role)과 권한(Authority):
 * - 역할: ROLE_ 접두사를 가진 권한의 묶음 (예: ROLE_USER, ROLE_ADMIN)
 * - 권한: 세밀한 단일 권한 (예: READ_ACCOUNT, DELETE_USER)
 * - hasRole("USER")는 내부적으로 "ROLE_USER" 권한을 체크
 * - hasAuthority("ROLE_USER")는 "ROLE_USER"를 그대로 체크
 * - 결과는 동일하지만 관례적으로 역할은 hasRole() 사용
 * 
 * DB 저장 예시:
 * - customers 테이블의 role 컬럼: "ROLE_USER", "ROLE_ADMIN"
 * - hasRole("USER")를 사용하면 Spring Security가 자동으로 "ROLE_" 접두사 추가
 * 
 * 테스트 방법:
 * # USER로 로그인
 * curl -u user@example.com:user123 http://localhost:8080/v12/authorization/user
 * → 200 OK
 * 
 * curl -u user@example.com:user123 http://localhost:8080/v12/authorization/admin
 * → 403 Forbidden
 * 
 * # ADMIN으로 로그인
 * curl -u admin@example.com:admin123 http://localhost:8080/v12/authorization/admin
 * → 200 OK
 * 
 * curl -u admin@example.com:admin123 http://localhost:8080/v12/authorization/user
 * → 403 Forbidden (ADMIN은 USER 역할이 없음)
 * 
 * # 인증 없이 접근
 * curl http://localhost:8080/v12/authorization/user
 * → 401 Unauthorized
 */
@Configuration
public class AuthorizationSecurityConfig {

    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService customUserDetailsService;

    @Bean
    @Order(12)
    public SecurityFilterChain authorizationSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v12/authorization/**", "/login", "/logout")
                
                // CSRF 비활성화 (테스트 편의)
                .csrf(csrf -> csrf.disable())
                
                // URL 기반 권한 제어 (세밀한 규칙)
                .authorizeHttpRequests(auth -> auth
                        // 1. 공개 엔드포인트: 인증 불필요
                        .requestMatchers("/v12/authorization/public").permitAll()
                        .requestMatchers("/v12/authorization/role-vs-authority").permitAll()
                        
                        // 2. USER 역할만 접근 가능
                        // hasRole("USER")는 "ROLE_USER" 권한을 체크
                        .requestMatchers("/v12/authorization/user").hasRole("USER")
                        
                        // 3. ADMIN 역할만 접근 가능
                        // hasRole("ADMIN")는 "ROLE_ADMIN" 권한을 체크
                        .requestMatchers("/v12/authorization/admin").hasRole("ADMIN")
                        
                        // 4. USER 또는 ADMIN 역할 중 하나만 있으면 접근 가능
                        // hasAnyRole()은 나열된 역할 중 하나라도 있으면 허용
                        .requestMatchers("/v12/authorization/any").hasAnyRole("USER", "ADMIN")
                        
                        // 5. 인증만 되면 역할 상관없이 접근 가능
                        // authenticated()는 특정 역할 체크 없이 인증된 사용자만 허용
                        .requestMatchers("/v12/authorization/authenticated").authenticated()
                        
                        // 6. 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                
                // Form Login 활성화
                .formLogin(withDefaults())
                
                // HTTP Basic 인증 활성화
                .httpBasic(withDefaults())
                
                // CustomUserDetailsService 사용
                // 이 서비스는 DB(InMemoryCustomerRepository)에서 사용자 정보를 로드하고
                // Customer의 role 필드를 GrantedAuthority로 변환
                .userDetailsService(customUserDetailsService);

        return http.build();
    }
}

