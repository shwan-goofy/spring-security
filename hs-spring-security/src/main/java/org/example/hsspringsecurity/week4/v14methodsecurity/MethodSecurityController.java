package org.example.hsspringsecurity.week4.v14methodsecurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V14: 메소드 레벨 보안 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. ADMIN 전용 메소드:
 *    curl -u admin@example.com:admin123 http://localhost:8080/v14/method/admin-only
 *    → 200 OK (ADMIN 역할 보유)
 *    
 *    curl -u user@example.com:user123 http://localhost:8080/v14/method/admin-only
 *    → 403 Forbidden (메소드 레벨에서 거부)
 * 
 * 2. 파라미터 기반 권한 체크:
 *    curl -u user@example.com:user123 http://localhost:8080/v14/method/owner-only/user@example.com
 *    → 200 OK (파라미터가 현재 사용자와 일치)
 *    
 *    curl -u user@example.com:user123 http://localhost:8080/v14/method/owner-only/other@example.com
 *    → 403 Forbidden (파라미터가 현재 사용자와 불일치)
 * 
 * 3. PostAuthorize 테스트:
 *    curl -u user@example.com:user123 http://localhost:8080/v14/method/post-check/true
 *    → 200 OK (반환 값의 public이 true)
 *    
 *    curl -u user@example.com:user123 http://localhost:8080/v14/method/post-check/false
 *    → 403 Forbidden (반환 값의 public이 false이고 ADMIN 아님)
 *    
 *    curl -u admin@example.com:admin123 http://localhost:8080/v14/method/post-check/false
 *    → 200 OK (ADMIN 역할 보유)
 * 
 * 4. 복잡한 조건:
 *    curl -u admin@example.com:admin123 http://localhost:8080/v14/method/complex/anyone
 *    → 200 OK (ADMIN이므로 username 무관)
 *    
 *    curl -u user@example.com:user123 http://localhost:8080/v14/method/complex/user@example.com
 *    → 200 OK (USER이고 자신의 username)
 *    
 *    curl -u user@example.com:user123 http://localhost:8080/v14/method/complex/other@example.com
 *    → 403 Forbidden (USER이지만 다른 사람의 username)
 * 
 * 학습 포인트:
 * - URL 레벨 보안과 메소드 레벨 보안의 차이
 * - Controller는 URL 레벨에서 authenticated()만 체크
 * - Service는 메소드 레벨에서 세밀한 권한 체크
 * - 심층 방어(Defense in Depth) 전략
 */
@RestController
@RequestMapping("/v14/method")
public class MethodSecurityController {

    @Autowired
    private MethodSecurityService methodSecurityService;

    /**
     * 공개 엔드포인트
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V14");
        response.put("message", "메소드 레벨 보안 - 공개 엔드포인트");
        response.put("authRequired", false);
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getName() : "anonymous");
        
        return response;
    }

    /**
     * ADMIN 역할만 접근 가능
     * 
     * Service 메소드에서 @PreAuthorize("hasRole('ADMIN')") 체크
     * Controller는 authenticated()만 체크하고, 실제 권한은 Service에서 체크
     */
    @GetMapping("/admin-only")
    public Map<String, Object> adminOnly() {
        // Service 메소드 호출 - 메소드 레벨에서 ADMIN 역할 체크
        Map<String, Object> data = methodSecurityService.getAdminData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V14");
        response.put("controller", "URL 레벨 체크: authenticated()");
        response.put("service", "메소드 레벨 체크: @PreAuthorize(\"hasRole('ADMIN')\")");
        response.put("data", data);
        response.put("testNote", "admin@example.com / admin123 로 로그인");
        
        return response;
    }

    /**
     * 파라미터 username이 현재 사용자와 같을 때만 접근 가능
     * 
     * Service 메소드에서 @PreAuthorize("#username == authentication.name") 체크
     */
    @GetMapping("/owner-only/{username}")
    public Map<String, Object> ownerOnly(@PathVariable String username) {
        // Service 메소드 호출 - 파라미터 기반 권한 체크
        Map<String, Object> data = methodSecurityService.getUserData(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V14");
        response.put("controller", "URL 레벨 체크: authenticated()");
        response.put("service", "메소드 레벨 체크: @PreAuthorize(\"#username == authentication.name\")");
        response.put("data", data);
        response.put("testNote", "자기 자신의 username으로만 접근 가능");
        
        return response;
    }

    /**
     * PostAuthorize 테스트
     * 
     * 메소드는 실행되지만, 반환 값의 'public' 필드에 따라 접근 제어
     */
    @GetMapping("/post-check/{isPublic}")
    public Map<String, Object> postCheck(@PathVariable boolean isPublic) {
        // Service 메소드 호출 - 실행 후 반환 값 기반 권한 체크
        Map<String, Object> data = methodSecurityService.getDataWithPostCheck(isPublic);
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V14");
        response.put("annotation", "@PostAuthorize");
        response.put("data", data);
        response.put("testNote", "isPublic=true이거나 ADMIN 역할이 있어야 데이터 반환");
        
        return response;
    }

    /**
     * 복잡한 조건 테스트
     */
    @GetMapping("/complex/{username}")
    public Map<String, Object> complex(@PathVariable String username) {
        Map<String, Object> data = methodSecurityService.getComplexData(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V14");
        response.put("data", data);
        
        return response;
    }

    /**
     * 공통 데이터 (USER 또는 ADMIN)
     */
    @GetMapping("/common")
    public Map<String, Object> common() {
        Map<String, Object> data = methodSecurityService.getCommonData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V14");
        response.put("data", data);
        
        return response;
    }

    /**
     * 메소드 보안 미적용 (비교용)
     */
    @GetMapping("/no-method-security")
    public Map<String, Object> noMethodSecurity() {
        Map<String, Object> data = methodSecurityService.getPublicData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V14");
        response.put("note", "Service 메소드에 권한 체크 어노테이션 없음");
        response.put("data", data);
        
        return response;
    }

    /**
     * URL 레벨 vs 메소드 레벨 보안 설명
     */
    @GetMapping("/security-levels")
    public Map<String, Object> securityLevels() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "V14");
        
        response.put("urlLevel", Map.of(
            "location", "SecurityFilterChain (Config)",
            "method", "authorizeHttpRequests()",
            "granularity", "URL 패턴 기반",
            "example", ".requestMatchers(\"/admin/**\").hasRole(\"ADMIN\")",
            "pros", "1차 방어선, 설정 집중화",
            "cons", "URL만 제어, 메소드 파라미터 체크 불가"
        ));
        
        response.put("methodLevel", Map.of(
            "location", "Service 메소드",
            "method", "@PreAuthorize, @PostAuthorize",
            "granularity", "메소드 및 파라미터 기반",
            "example", "@PreAuthorize(\"#id == authentication.principal.id\")",
            "pros", "2차 방어선, 세밀한 제어, 비즈니스 로직과 가까움",
            "cons", "서비스 레이어에 보안 로직 분산"
        ));
        
        response.put("recommendation", "두 레벨 모두 사용하여 심층 방어(Defense in Depth) 전략 구현");
        
        return response;
    }
}

