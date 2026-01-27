package org.example.hsspringsecurity.week2.v10failure;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V10: Custom Authentication Failure Handler 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. 브라우저 테스트 (Form Login):
 *    - 브라우저로 http://localhost:8080/v10/failure/secured 접근
 *    - 로그인 페이지로 리다이렉트됨
 *    - 잘못된 비밀번호 입력 (예: admin@example.com / wrongpassword)
 *    - JSON 오류 응답 확인:
 *      {
 *        "error": "Unauthorized",
 *        "errorType": "BadCredentialsException",
 *        "message": "Invalid username or password",
 *        "timestamp": "2024-01-27T10:30:00",
 *        "path": "/login"
 *      }
 * 
 * 2. HTTP Basic 테스트:
 *    curl -u admin@example.com:wrongpassword http://localhost:8080/v10/failure/secured
 *    → 401 Unauthorized (HTTP Basic은 failure handler가 적용되지 않음)
 * 
 * 3. 정상 로그인:
 *    curl -u admin@example.com:admin123 http://localhost:8080/v10/failure/secured
 *    → 200 OK
 * 
 * 학습 포인트:
 * - 인증 실패 시 명확한 피드백 제공
 * - 예외 타입별로 다른 메시지 제공 가능
 * - 보안: 실제 운영에서는 세부 정보 노출 최소화
 */
@RestController
@RequestMapping("/v10/failure")
public class FailureHandlerController {
    
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V10");
        response.put("message", "Custom Failure Handler 공개 엔드포인트");
        response.put("description", "인증 없이 접근 가능합니다");
        response.put("features", "커스텀 인증 실패 핸들러");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("principal", authentication != null ? authentication.getPrincipal() : "null");
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        
        return response;
    }
    
    @GetMapping("/secured")
    public Map<String, Object> securedEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V10");
        response.put("message", "Custom Failure Handler 보호된 엔드포인트");
        response.put("description", "인증된 사용자만 접근 가능합니다");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("authenticationProvider", "DaoAuthenticationProvider");
        response.put("failureHandler", "CustomAuthenticationFailureHandler");
        
        response.put("testGuide", Map.of(
            "1", "브라우저로 이 URL 접근",
            "2", "잘못된 비밀번호로 로그인 시도",
            "3", "JSON 형태의 오류 응답 확인"
        ));
        
        response.put("learningNote", "CustomAuthenticationFailureHandler는 사용자 친화적인 오류 메시지를 제공합니다");
        
        return response;
    }
}

