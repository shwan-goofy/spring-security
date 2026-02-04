package org.example.hsspringsecurity.week5.v16resourceserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * V16: OAuth2 Resource Server 보안 설정
 * 
 * WEEK 5 학습 목표:
 * - Resource Server 개념 이해
 * - JwtDecoder Bean으로 JWT 검증
 * - BearerTokenAuthenticationFilter 자동 생성
 * - Authorization Server와 Resource Server 분리 아키텍처
 * 
 * SecurityFilterChain 특징:
 * - @Order(16): V15 다음 순서
 * - securityMatcher("/v16/resource/**"): 경로 매칭
 * - sessionManagement(STATELESS): 세션 사용 안 함
 * - oauth2ResourceServer(jwt()): Resource Server 모드 활성화
 * - JwtDecoder Bean: JWT 검증 로직
 * - JwtAuthenticationConverter: JWT Claims → GrantedAuthority 변환
 * 
 * Resource Server 필터 체인:
 * 1. SecurityContextHolderFilter (순서 3)
 * 2. HeaderWriterFilter (순서 4)
 * 3. (CorsFilter - 설정 시 활성화)
 * 4. (CsrfFilter 제거 - csrf().disable())
 * 5. LogoutFilter (순서 7)
 * 6. BearerTokenAuthenticationFilter ← oauth2ResourceServer()로 자동 추가
 *    역할: Authorization 헤더에서 JWT 추출 및 검증
 *    위치: 인증 필터 사이 (자동 배치)
 * 7. RequestCacheAwareFilter (순서 14)
 * 8. AnonymousAuthenticationFilter (순서 17)
 * 9. (SessionManagementFilter - STATELESS 모드)
 * 10. ExceptionTranslationFilter (순서 20)
 * 11. AuthorizationFilter (순서 21)
 * 
 * BearerTokenAuthenticationFilter 동작 플로우:
 * 
 * [JWT 검증 성공]
 * 1. Authorization 헤더에서 JWT 추출
 *    예: "Authorization: eyJhbGc..."
 * 
 * 2. JwtDecoder.decode(jwt) 호출
 *    - 서명 검증 (Secret Key 또는 Public Key)
 *    - 만료 시간 검증 (exp claim)
 *    - Claims 추출
 * 
 * 3. JwtAuthenticationConverter 호출
 *    - JWT Claims → GrantedAuthority 변환
 *    - "authorities" claim을 읽어 권한 목록 생성
 *    - 예: "ROLE_USER,ROLE_ADMIN" → [ROLE_USER, ROLE_ADMIN]
 * 
 * 4. JwtAuthenticationToken 생성
 *    - principal: Jwt 객체
 *    - credentials: null
 *    - authorities: 변환된 권한 목록
 *    - authenticated: true
 * 
 * 5. SecurityContext에 저장
 *    - SecurityContextHolder.getContext().setAuthentication(token)
 * 
 * 6. 다음 필터로 전달
 * 
 * [JWT 검증 실패]
 * 1. JwtDecoder에서 예외 발생
 *    - JwtException: 서명 불일치, 만료, 형식 오류 등
 * 
 * 2. ExceptionTranslationFilter가 처리
 *    - AuthenticationEntryPoint 호출
 *    - 401 Unauthorized 응답
 * 
 * V13(Authorization Server) vs V16(Resource Server):
 * 
 * [V13 - JWT 발급]
 * - 사용자 인증 (BasicAuthenticationFilter)
 * - JWT 생성 (JwtTokenGeneratorFilter)
 * - 응답 헤더에 JWT 추가
 * - 역할: Authorization Server
 * 
 * [V16 - JWT 검증]
 * - JWT 검증 (BearerTokenAuthenticationFilter + JwtDecoder)
 * - API 제공
 * - 역할: Resource Server
 * 
 * JwtDecoder Bean의 중요성:
 * - oauth2ResourceServer() 설정 시 필수
 * - 이 Bean이 없으면 애플리케이션 시작 실패
 * - MockJwtDecoder: V13과 같은 Secret Key 사용
 * - 실무: NimbusJwtDecoder.withJwkSetUri() 사용
 * 
 * 실무 설정 예시 (Keycloak 연동):
 * @Bean
 * public JwtDecoder jwtDecoder() {
 *     return NimbusJwtDecoder.withJwkSetUri(
 *         "http://localhost:8180/realms/eazybank/protocol/openid-connect/certs"
 *     ).build();
 * }
 * 
 * 또는 application.yml:
 * spring:
 *   security:
 *     oauth2:
 *       resourceserver:
 *         jwt:
 *           issuer-uri: http://localhost:8180/realms/eazybank
 * 
 * 테스트 방법:
 * 1. V13에서 JWT 발급:
 *    curl -u user@example.com:user123 http://localhost:8080/v13/jwt/login -v
 *    → Authorization 헤더에서 JWT 복사
 * 
 * 2. V16에서 JWT로 API 호출:
 *    curl http://localhost:8080/v16/resource/api/account \
 *      -H "Authorization: (복사한 JWT)"
 *    → 200 OK
 * 
 * 3. 권한 테스트:
 *    curl http://localhost:8080/v16/resource/api/admin \
 *      -H "Authorization: (USER JWT)"
 *    → 403 Forbidden
 */
@Configuration
public class ResourceServerSecurityConfig {

    @Autowired
    private MockJwtDecoder mockJwtDecoder;

    @Bean
    @Order(16)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v16/resource/**")
                
                // 세션 관리: STATELESS 모드
                // JWT 기반이므로 세션 사용 안 함
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // CSRF 보호 비활성화
                // JWT를 Authorization 헤더에 담아 보내므로 CSRF 공격에 안전
                .csrf(csrf -> csrf.disable())
                
                // URL 기반 접근 제어
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트
                        .requestMatchers(
                            "/v16/resource/public",
                            "/v16/resource/concept",
                            "/v16/resource/jwt-errors"
                        ).permitAll()
                        
                        // ADMIN 전용 API
                        .requestMatchers("/v16/resource/api/admin").hasRole("ADMIN")
                        
                        // USER 또는 ADMIN API
                        .requestMatchers("/v16/resource/api/**").hasAnyRole("USER", "ADMIN")
                        
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                
                // OAuth2 Resource Server 활성화
                // BearerTokenAuthenticationFilter 자동 추가
                .oauth2ResourceServer(oauth2 -> oauth2
                        // JWT 검증 설정
                        .jwt(jwt -> jwt
                                // 커스텀 JwtDecoder 주입
                                .decoder(mockJwtDecoder)
                                
                                // JWT Claims → GrantedAuthority 변환
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    /**
     * JWT Claims를 GrantedAuthority로 변환하는 Converter
     * 
     * V13에서 JWT 생성 시:
     * .claim("authorities", "ROLE_USER,ROLE_ADMIN")
     * 
     * V16에서 이 Converter가:
     * "ROLE_USER,ROLE_ADMIN" → [SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("ROLE_ADMIN")]
     * 
     * 실무에서는 JWT의 구조에 맞게 커스터마이징:
     * - Keycloak: realm_access.roles
     * - Auth0: permissions
     * - 커스텀: authorities
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // JWT의 "authorities" claim을 읽어 GrantedAuthority로 변환
        converter.setJwtGrantedAuthoritiesConverter(new Converter<Jwt, Collection<GrantedAuthority>>() {
            @Override
            public Collection<GrantedAuthority> convert(Jwt jwt) {
                // V13에서 저장한 "authorities" claim 읽기
                // 예: "ROLE_USER,ROLE_ADMIN"
                String authoritiesString = jwt.getClaim("authorities");
                
                if (authoritiesString == null || authoritiesString.isEmpty()) {
                    return java.util.Collections.emptyList();
                }
                
                // 쉼표로 구분된 문자열을 GrantedAuthority 리스트로 변환
                return Arrays.stream(authoritiesString.split(","))
                        .map(String::trim)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
        });
        
        return converter;
    }
}

