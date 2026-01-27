package org.example.hsspringsecurity.week2.v10failure;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 커스텀 인증 실패 핸들러
 * 
 * WEEK 2 학습 목표:
 * - 인증 실패 시 사용자 친화적 오류 응답 제공
 * - 예외 타입별로 다른 메시지 제공
 * - JSON 형태로 오류 정보 반환
 * 
 * 처리하는 예외 타입:
 * - BadCredentialsException: 잘못된 비밀번호
 * - UsernameNotFoundException: 존재하지 않는 사용자
 * - DisabledException: 비활성화된 계정
 * - LockedException: 잠긴 계정
 * 
 * 보안 주의사항:
 * - 실제 운영 환경에서는 "사용자 없음" vs "비밀번호 틀림"을 구분하지 않음
 * - 공격자에게 힌트를 주지 않기 위해 "Invalid username or password" 로 통일
 */
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                       HttpServletResponse response,
                                       AuthenticationException exception)
            throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        String errorMessage = determineErrorMessage(exception);
        String errorType = exception.getClass().getSimpleName();
        
        // JSON 응답 생성
        String json = String.format(
            "{\"error\":\"Unauthorized\",\"errorType\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
            errorType,
            errorMessage,
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            request.getRequestURI()
        );
        
        response.getWriter().write(json);
        
        // 로그 출력 (학습용)
        System.out.println("=== 인증 실패 ===");
        System.out.println("Exception Type: " + errorType);
        System.out.println("Message: " + errorMessage);
        System.out.println("Path: " + request.getRequestURI());
    }
    
    /**
     * 예외 타입별로 적절한 오류 메시지 반환
     */
    private String determineErrorMessage(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            // 실제 운영 환경에서는 보안을 위해 세부 정보 노출 최소화
            return "Invalid username or password";
            
        } else if (exception instanceof UsernameNotFoundException) {
            // 보안: 사용자 존재 여부를 노출하지 않음
            return "Invalid username or password";
            
        } else if (exception instanceof DisabledException) {
            return "Account is disabled. Please contact administrator.";
            
        } else if (exception instanceof LockedException) {
            return "Account is locked. Please contact administrator.";
            
        } else {
            return "Authentication failed: " + exception.getMessage();
        }
    }
}

