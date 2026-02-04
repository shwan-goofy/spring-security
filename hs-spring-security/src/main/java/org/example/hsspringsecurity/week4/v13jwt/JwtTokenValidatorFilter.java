package org.example.hsspringsecurity.week4.v13jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 토큰 검증 필터
 * 
 * 역할:
 * - 클라이언트가 보낸 JWT 토큰을 검증
 * - 토큰이 유효하면 Authentication 객체를 생성하여 SecurityContext에 저장
 * - UsernamePasswordAuthenticationFilter 이전에 실행되도록 설정
 * 
 * 동작 플로우:
 * 1. 클라이언트가 보호된 리소스(/v13/jwt/secured)에 JWT와 함께 요청
 * 2. 이 필터가 Authorization 헤더에서 JWT 추출
 * 3. SecretKey로 JWT 서명 검증
 * 4. 검증 성공 시 Claims에서 사용자 정보(username, authorities) 추출
 * 5. Authentication 객체 생성 후 SecurityContext에 저장
 * 6. 이후 필터들은 이미 인증된 사용자로 처리
 * 
 * 검증 단계:
 * 1. 서명 검증: JWT가 위변조되지 않았는지 확인
 * 2. 만료 시간 검증: exp claim이 현재 시간보다 미래인지 확인
 * 3. Claims 추출: username, authorities 등 커스텀 claim 읽기
 * 
 * 필터 체인 위치:
 * JwtTokenValidatorFilter (커스텀, 순서 8-9 사이) → UsernamePasswordAuthenticationFilter (순서 9)
 * 
 * 학습 포인트:
 * - JWT 검증은 별도의 인증 프로세스 없이 토큰만으로 완료
 * - 서버는 상태를 저장하지 않음 (Stateless)
 * - 매 요청마다 JWT 검증 수행
 * - BadCredentialsException 발생 시 ExceptionTranslationFilter가 처리
 */
public class JwtTokenValidatorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Authorization 헤더에서 JWT 토큰 가져오기
        String jwt = request.getHeader(SecurityConstants.JWT_HEADER);
        
        // JWT가 존재하는 경우에만 검증 수행
        if (jwt != null) {
            try {
                // 1. 서버만 아는 비밀 키로 SecretKey 객체 생성
                SecretKey key = Keys.hmacShaKeyFor(
                        SecurityConstants.JWT_KEY.getBytes(StandardCharsets.UTF_8));

                // 2. JWT 파싱 및 서명 검증
                // verifyWith(key): 서명 검증에 사용할 키 지정
                // build(): JwtParser 생성
                // parseSignedClaims(jwt): JWT 파싱 및 검증 실행
                // 검증 실패 시 JwtException 발생 (서명 불일치, 만료 등)
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(jwt)
                        .getPayload();
                
                // 3. Claims에서 사용자 정보 추출
                // JwtTokenGeneratorFilter에서 저장한 커스텀 claim 읽기
                String username = String.valueOf(claims.get("username"));
                String authorities = (String) claims.get("authorities");
                
                // 4. Authentication 객체 생성
                // UsernamePasswordAuthenticationToken: Spring Security의 표준 인증 토큰
                // principal: username
                // credentials: null (JWT 검증 완료 후 비밀번호 불필요)
                // authorities: 쉼표로 구분된 문자열을 GrantedAuthority 리스트로 변환
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        username, 
                        null,
                        AuthorityUtils.commaSeparatedStringToAuthorityList(authorities)
                );
                
                // 5. SecurityContext에 Authentication 저장
                // 이후 필터들은 이미 인증된 사용자로 처리
                SecurityContextHolder.getContext().setAuthentication(auth);
                
            } catch (Exception e) {
                // JWT 검증 실패 (서명 불일치, 만료, 형식 오류 등)
                // BadCredentialsException을 발생시켜 ExceptionTranslationFilter가 처리하도록 함
                throw new BadCredentialsException("Invalid Token received!", e);
            }
        }
        
        // 다음 필터로 요청/응답 전달
        // JWT가 유효하면 SecurityContext에 Authentication이 저장된 상태
        filterChain.doFilter(request, response);
    }

    /**
     * 이 필터를 실행하지 않을 조건 설정
     * 
     * /v13/jwt/login 경로에서는 JWT 검증 필터 실행 안 함
     * (로그인 시에는 JWT가 없고, BasicAuthenticationFilter가 인증 처리)
     * 
     * @return true면 필터 실행 안 함, false면 실행
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /v13/jwt/login에서는 필터 실행 안 함
        return request.getServletPath().equals("/v13/jwt/login");
    }
}

