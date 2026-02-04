package org.example.hsspringsecurity.week5.v16resourceserver;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.hsspringsecurity.week4.v13jwt.SecurityConstants;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Mock JWT Decoder
 * 
 * 역할:
 * - BearerTokenAuthenticationFilter가 JWT를 검증할 때 사용
 * - V13에서 생성한 JWT를 V16에서 검증
 * - 실제 환경에서는 Authorization Server의 Public Key 사용
 * - Mock 환경에서는 V13과 같은 Secret Key 사용
 * 
 * JwtDecoder 인터페이스:
 * - Spring Security OAuth2 Resource Server의 핵심 컴포넌트
 * - decode(String token) 메소드 구현 필요
 * - JWT 검증 실패 시 JwtException 발생
 * 
 * 실무 환경 vs Mock 환경:
 * 
 * [실무 - Keycloak/Auth0 연동]
 * 1. JwtDecoder Bean 설정:
 *    @Bean
 *    public JwtDecoder jwtDecoder() {
 *        return NimbusJwtDecoder.withJwkSetUri(
 *            "http://localhost:8180/realms/eazybank/protocol/openid-connect/certs"
 *        ).build();
 *    }
 * 
 * 2. 최초 요청 시 Authorization Server에서 JWK(JSON Web Key) 조회
 * 3. Public Key로 JWT 서명 검증
 * 4. JWK 캐시 (만료 시 재조회)
 * 
 * [Mock - V13과 연동]
 * 1. V13과 동일한 Secret Key 사용
 * 2. JJWT 라이브러리로 직접 검증
 * 3. 외부 호출 없음 (빠른 테스트)
 * 
 * JWT 검증 단계:
 * 1. 서명(Signature) 검증: 토큰이 위변조되지 않았는지 확인
 * 2. 만료 시간(exp) 검증: 토큰이 아직 유효한지 확인
 * 3. 발급자(iss) 검증: 신뢰할 수 있는 서버가 발급했는지 확인 (선택)
 * 4. 대상(aud) 검증: 이 서버를 위해 발급된 토큰인지 확인 (선택)
 * 
 * Spring Security의 Jwt vs JJWT의 Claims:
 * - JJWT: JWT 파싱/생성 라이브러리
 * - Spring Security Jwt: OAuth2 Resource Server에서 사용하는 표준 인터페이스
 * - 이 클래스는 JJWT로 파싱한 후 Spring Security Jwt로 변환
 * 
 * 학습 포인트:
 * - Resource Server는 JWT 검증만 수행 (발급 안 함)
 * - BearerTokenAuthenticationFilter가 자동으로 이 Decoder 사용
 * - JWT가 유효하면 JwtAuthenticationToken 생성
 * - 실무에서는 NimbusJwtDecoder 사용 권장
 */
@Component
public class MockJwtDecoder implements JwtDecoder {

    /**
     * V13과 동일한 Secret Key 사용
     * 실무에서는 Authorization Server의 Public Key 사용
     */
    private final SecretKey key = Keys.hmacShaKeyFor(
        SecurityConstants.JWT_KEY.getBytes(StandardCharsets.UTF_8)
    );

    /**
     * JWT 토큰 검증 및 디코딩
     * 
     * @param token JWT 토큰 문자열
     * @return Spring Security Jwt 객체
     * @throws JwtException JWT 검증 실패 시
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // 1. JJWT 라이브러리로 JWT 파싱 및 서명 검증
            // verifyWith(key): 서명 검증에 사용할 키 지정
            // build(): JwtParser 생성
            // parseSignedClaims(token): JWT 파싱 및 검증 실행
            // 검증 실패 시 io.jsonwebtoken.JwtException 발생:
            //   - ExpiredJwtException: 토큰 만료
            //   - SignatureException: 서명 불일치
            //   - MalformedJwtException: 잘못된 형식
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // 2. JJWT Claims를 Spring Security Jwt 객체로 변환
            // Jwt.Builder 패턴 사용
            return Jwt.withTokenValue(token)
                    // Header 설정
                    .header("alg", "HS256")
                    .header("typ", "JWT")
                    
                    // Standard Claims (RFC 7519)
                    .issuer(claims.getIssuer())
                    .subject(claims.getSubject())
                    .issuedAt(claims.getIssuedAt().toInstant())
                    .expiresAt(claims.getExpiration().toInstant())
                    
                    // Custom Claims (V13에서 추가한 것)
                    .claim("username", claims.get("username"))
                    .claim("authorities", claims.get("authorities"))
                    
                    // Jwt 객체 생성
                    .build();
                    
        } catch (io.jsonwebtoken.JwtException e) {
            // JJWT 예외를 Spring Security JwtException으로 변환
            throw new JwtException("Invalid JWT token: " + e.getMessage(), e);
        } catch (Exception e) {
            // 기타 예외 처리
            throw new JwtException("Failed to decode JWT token", e);
        }
    }
}

