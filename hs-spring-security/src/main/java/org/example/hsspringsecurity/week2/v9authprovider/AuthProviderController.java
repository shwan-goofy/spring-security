package org.example.hsspringsecurity.week2.v9authprovider;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V9: Custom AuthenticationProvider 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. curl http://localhost:8080/v9/authprovider/public
 *    → 200 OK (인증 불필요)
 * 
 * 2. curl -u admin@example.com:admin123 http://localhost:8080/v9/authprovider/secured
 *    → 200 OK (도메인 검증 통과)
 * 
 * 3. curl -u user@example.com:user123 http://localhost:8080/v9/authprovider/business-check
 *    → 200 OK (도메인 검증 통과)
 * 
 * 4. curl -u test@wrongdomain.com:test123 http://localhost:8080/v9/authprovider/secured
 *    → 401 Unauthorized (비즈니스 로직 검증 실패: @example.com 도메인 아님)
 *    주의: 이 사용자는 DB에 없으므로 실제로는 "No user registered" 오류 발생
 * 
 * 5. 새 사용자 등록 후 테스트:
 *    curl -X POST -H "Content-Type: application/json" \
 *      -d '{"email":"test@wrongdomain.com","pwd":"test123","name":"Wrong Domain"}' \
 *      http://localhost:8080/v8/register/signup
 *    
 *    curl -u test@wrongdomain.com:test123 http://localhost:8080/v9/authprovider/secured
 *    → 401 Unauthorized (비밀번호는 맞지만 도메인 검증 실패)
 * 
 * 학습 포인트:
 * - CustomAuthenticationProvider가 모든 인증 로직 제어
 * - 비밀번호 검증 + 추가 비즈니스 로직 (도메인 체크) 수행
 * - UserDetailsService보다 유연하지만 코드 복잡도 증가
 */
@RestController
@RequestMapping("/v9/authprovider")
public class AuthProviderController {
    
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V9");
        response.put("message", "Custom AuthenticationProvider 공개 엔드포인트");
        response.put("description", "인증 없이 접근 가능합니다");
        response.put("approachType", "Custom AuthenticationProvider (비즈니스 로직 포함)");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("principal", authentication != null ? authentication.getPrincipal() : "null");
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        
        return response;
    }
    
    @GetMapping("/secured")
    public Map<String, Object> securedEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V9");
        response.put("message", "Custom AuthenticationProvider 보호된 엔드포인트");
        response.put("description", "인증된 사용자만 접근 가능합니다");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("authenticationProvider", "CustomAuthenticationProvider (직접 구현)");
        
        response.put("businessRules", "✅ 비밀번호 검증 + ✅ @example.com 도메인 체크");
        
        return response;
    }
    
    @GetMapping("/business-check")
    public Map<String, Object> businessCheckEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V9");
        response.put("message", "비즈니스 로직 검증 확인");
        response.put("description", "CustomAuthenticationProvider의 추가 검증을 통과했습니다");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        
        String email = authentication.getName();
        String domain = email.substring(email.indexOf("@"));
        
        response.put("emailDomain", domain);
        response.put("domainCheckResult", "✅ @example.com 도메인 확인됨");
        
        response.put("learningNote", Map.of(
            "UserDetailsService 방식", "비밀번호 검증만 가능",
            "AuthenticationProvider 방식", "비밀번호 검증 + 추가 비즈니스 로직 (도메인, IP, 시간 등) 가능",
            "주의사항", "AuthenticationProvider Bean 등록 시 UserDetailsService는 무시됨 (배타적 관계)"
        ));
        
        return response;
    }
}

