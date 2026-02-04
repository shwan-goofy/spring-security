package org.example.hsspringsecurity.week3.v11corscrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CSRF 토큰을 응답 헤더에 추가하는 커스텀 필터
 * 
 * 역할:
 * - CsrfFilter가 생성한 CSRF 토큰을 응답 헤더에 명시적으로 추가
 * - 프론트엔드(React, Angular)가 쿠키뿐만 아니라 헤더로도 토큰 값 확인 가능
 * 
 * 동작 원리:
 * 1. CsrfFilter가 요청을 처리하면서 CSRF 토큰을 생성하여 request attribute에 저장
 * 2. 이 필터가 BasicAuthenticationFilter 이후에 실행되면서 토큰을 응답 헤더에 추가
 * 3. 클라이언트는 Set-Cookie 헤더(XSRF-TOKEN)와 커스텀 헤더(X-XSRF-TOKEN) 모두에서 토큰 확인 가능
 * 
 * 필터 체인 위치:
 * CsrfFilter (순서 6) → ... → BasicAuthenticationFilter (순서 12) → CsrfCookieFilter (커스텀)
 * 
 * 학습 포인트:
 * - OncePerRequestFilter: 한 요청당 한 번만 실행되도록 보장
 * - CsrfToken은 request.getAttribute()로 가져올 수 있음
 * - 응답 헤더 추가는 filterChain.doFilter() 호출 전에 수행
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                     FilterChain filterChain) throws ServletException, IOException {
        
        // CsrfFilter가 생성한 CSRF 토큰을 request attribute에서 가져온다
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        
        if (csrfToken != null) {
            // CSRF 토큰을 응답 헤더에 추가
            // 헤더 이름: X-XSRF-TOKEN (기본값)
            // 프론트엔드는 이 헤더 또는 쿠키에서 토큰 값을 읽을 수 있음
            response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());
        }
        
        // 다음 필터로 요청/응답 전달
        filterChain.doFilter(request, response);
    }
}

