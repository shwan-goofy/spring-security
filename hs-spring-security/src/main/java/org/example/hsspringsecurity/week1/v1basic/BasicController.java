package org.example.hsspringsecurity.week1.v1basic;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V1 - 최소 설정 컨트롤러
 * 
 * 테스트 시나리오:
 * 1. GET /v1/basic/public → 200 OK (인증 불필요)
 * 2. GET /v1/basic/secured → 403 Forbidden (인증 방법이 없음)
 * 3. GET /v1/basic/user → 403 Forbidden
 * 4. GET /v1/basic/admin → 403 Forbidden
 * 
 * 예상 결과:
 * - /public만 접근 가능
 * - 나머지는 모두 403 Forbidden (인증 필터가 없어서 로그인 불가능)
 * - 401 Unauthorized가 아닌 403 Forbidden인 이유:
 *   AnonymousAuthenticationFilter가 익명 사용자 권한을 부여하지만,
 *   authenticated()는 실제 인증된 사용자만 허용하므로 접근 거부됨
 */
@RestController
@RequestMapping("/v1/basic")
public class BasicController {

    /**
     * 공개 엔드포인트 - 인증 불필요
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V1 - 공개 엔드포인트");
        response.put("description", "인증 없이 접근 가능합니다");
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getName() : "anonymous");
        response.put("authorities", auth != null ? auth.getAuthorities() : null);
        
        return response;
    }

    /**
     * 보호된 엔드포인트 - 인증 필요
     * 
     * 예상 결과: 403 Forbidden
     * 이유: 인증 방법(formLogin, httpBasic)이 없어서 로그인 불가능
     */
    @GetMapping("/secured")
    public Map<String, Object> securedEndpoint(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V1 - 보호된 엔드포인트");
        response.put("description", "인증된 사용자만 접근 가능");
        response.put("username", userDetails != null ? userDetails.getUsername() : "N/A");
        response.put("authorities", userDetails != null ? userDetails.getAuthorities() : null);
        
        return response;
    }

    /**
     * 사용자 엔드포인트 - 인증 필요
     * 
     * 예상 결과: 403 Forbidden
     */
    @GetMapping("/user")
    public Map<String, Object> userEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V1 - 사용자 엔드포인트");
        response.put("username", auth != null ? auth.getName() : "N/A");
        response.put("authorities", auth != null ? auth.getAuthorities() : null);
        
        return response;
    }

    /**
     * 관리자 엔드포인트 - 인증 필요
     * 
     * 예상 결과: 403 Forbidden
     * 참고: 현재는 권한 검사가 없지만, WEEK 3에서 hasRole("ADMIN") 추가 예정
     */
    @GetMapping("/admin")
    public Map<String, Object> adminEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "V1 - 관리자 엔드포인트");
        response.put("username", auth != null ? auth.getName() : "N/A");
        response.put("authorities", auth != null ? auth.getAuthorities() : null);
        
        return response;
    }
}

