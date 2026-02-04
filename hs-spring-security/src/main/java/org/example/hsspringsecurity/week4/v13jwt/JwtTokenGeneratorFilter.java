package org.example.hsspringsecurity.week4.v13jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT 토큰 생성 필터
 * 
 * 역할:
 * - 사용자가 성공적으로 인증된 후 JWT 토큰을 생성
 * - 생성된 토큰을 응답 헤더에 추가하여 클라이언트에게 전달
 * - BasicAuthenticationFilter 이후에 실행되도록 설정
 * 
 * 동작 플로우:
 * 1. 클라이언트가 /v13/jwt/login에 HTTP Basic Auth로 요청
 * 2. BasicAuthenticationFilter가 인증 수행
 * 3. 인증 성공 시 SecurityContext에 Authentication 객체 저장
 * 4. 이 필터가 실행되면서 Authentication 객체를 JWT로 변환
 * 5. JWT를 응답 헤더(Authorization)에 추가
 * 6. 클라이언트는 이후 요청에서 이 JWT를 사용
 * 
 * JWT Payload 구조:
 * {
 *   "iss": "hs-spring-security",        // Issuer (발급자)
 *   "sub": "JWT Token",                 // Subject (주제)
 *   "username": "user@example.com",     // 사용자 이름 (커스텀 claim)
 *   "authorities": "ROLE_USER,ROLE_...", // 권한 목록 (커스텀 claim)
 *   "iat": 1609459200,                  // Issued At (발급 시간)
 *   "exp": 1609462800                   // Expiration (만료 시간)
 * }
 * 
 * 필터 체인 위치:
 * BasicAuthenticationFilter (순서 12) → JwtTokenGeneratorFilter (커스텀, 순서 12-13 사이)
 * 
 * 학습 포인트:
 * - OncePerRequestFilter: 한 요청당 한 번만 실행
 * - shouldNotFilter(): 특정 경로에서만 필터 실행 제어
 * - SecurityContextHolder: ThreadLocal에서 Authentication 가져오기
 * - JJWT 라이브러리 사용법
 */
public class JwtTokenGeneratorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                     FilterChain filterChain) throws ServletException, IOException {
        
        // SecurityContext에서 인증 정보 가져오기
        // BasicAuthenticationFilter가 이미 인증을 완료한 상태
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증이 성공한 경우에만 JWT 생성
        if (authentication != null) {
            // JWT 서명에 사용할 SecretKey 생성
            // HS256 알고리즘 사용 (HMAC-SHA256)
            SecretKey key = Keys.hmacShaKeyFor(SecurityConstants.JWT_KEY.getBytes(StandardCharsets.UTF_8));
            
            // JWT 토큰 생성
            String jwt = Jwts.builder()
                    // 발급자(Issuer) 설정
                    .issuer("hs-spring-security")
                    
                    // 주제(Subject) 설정
                    .subject("JWT Token")
                    
                    // 커스텀 Claim: 사용자 이름
                    .claim("username", authentication.getName())
                    
                    // 커스텀 Claim: 권한 목록 (쉼표로 구분된 문자열)
                    // 예: "ROLE_USER,ROLE_ADMIN"
                    .claim("authorities", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(",")))
                    
                    // 발급 시간 (Issued At)
                    .issuedAt(new Date())
                    
                    // 만료 시간 (Expiration) - 현재 시간 + 1시간
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    
                    // 서명 (Signature) - SecretKey로 서명
                    .signWith(key)
                    
                    // JWT 문자열로 직렬화
                    .compact();
            
            // 생성된 JWT를 응답 헤더에 추가
            // 클라이언트는 이 헤더에서 JWT를 읽어 저장
            response.setHeader(SecurityConstants.JWT_HEADER, jwt);
        }
        
        // 다음 필터로 요청/응답 전달
        filterChain.doFilter(request, response);
    }

    /**
     * 이 필터를 실행하지 않을 조건 설정
     * 
     * /v13/jwt/login 경로에서만 JWT 생성 필터 실행
     * 다른 경로에서는 실행하지 않음
     * 
     * @return true면 필터 실행 안 함, false면 실행
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /v13/jwt/login이 아닌 경로에서는 필터 실행 안 함
        return !request.getServletPath().equals("/v13/jwt/login");
    }
}

