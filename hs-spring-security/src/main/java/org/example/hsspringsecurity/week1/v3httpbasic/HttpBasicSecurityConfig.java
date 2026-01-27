package org.example.hsspringsecurity.week1.v3httpbasic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * V3 - HTTP Basic 인증 추가
 * 
 * URL 패턴: /v3/httpbasic/**
 * 
 * 학습 목표:
 * - httpBasic() 설정 시 BasicAuthenticationFilter가 추가됨을 확인
 * - Authorization: Basic 헤더를 통한 인증 처리
 * - Stateless한 인증 방식 이해 (세션 사용하지만 매 요청마다 인증 가능)
 * - Postman/curl로 테스트
 * 
 * V1, V2와의 차이점:
 * - .httpBasic(withDefaults()) 추가
 * 
 * 추가되는 필터:
 * 12. BasicAuthenticationFilter - Authorization: Basic 헤더 처리
 * 
 * BasicAuthenticationFilter 동작 과정:
 * 1. Authorization 헤더 확인 (Basic base64(username:password))
 * 2. Base64 디코딩하여 username, password 추출
 * 3. UsernamePasswordAuthenticationToken (미인증) 생성
 * 4. AuthenticationManager에 인증 위임
 * 5. DaoAuthenticationProvider가 인증 처리 (V2와 동일한 흐름)
 * 6. 인증 성공 시 SecurityContextHolder에 저장
 * 7. 세션에도 저장 (기본 동작)
 * 
 * HTTP Basic 인증의 특징:
 * - 매 요청마다 Authorization 헤더에 자격 증명 포함
 * - HTTPS 필수 (암호화되지 않은 HTTP에서는 평문으로 전송됨)
 * - 브라우저 기본 지원 (팝업 로그인 창)
 * - REST API에 적합 (폼 로그인보다 간단)
 * - 로그아웃 불가능 (브라우저가 자격 증명을 캐시)
 */
@Configuration
public class HttpBasicSecurityConfig {

    @Bean
    @Order(3) // V2 다음 우선순위
    public SecurityFilterChain httpBasicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // 이 SecurityFilterChain은 /v3/httpbasic/** 경로에만 적용
            .securityMatcher("/v3/httpbasic/**")
            
            // URL 기반 접근 제어
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/httpbasic/public").permitAll() // 인증 불필요
                .anyRequest().authenticated() // 나머지는 모두 인증 필요
            )
            
            // HTTP Basic 인증 활성화 (기본 설정 사용)
            // → BasicAuthenticationFilter 추가
            // → Authorization: Basic base64(username:password) 헤더 처리
            // → 인증 실패 시 401 Unauthorized + WWW-Authenticate: Basic realm="..." 헤더 반환
            .httpBasic(withDefaults());
        
        return http.build();
    }
}

