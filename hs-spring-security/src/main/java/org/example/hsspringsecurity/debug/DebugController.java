package org.example.hsspringsecurity.debug;

import jakarta.servlet.Filter;
import org.example.hsspringsecurity.common.domain.Customer;
import org.example.hsspringsecurity.common.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Debug 컨트롤러
 * 
 * 목적:
 * - Spring Security 필터 체인 정보 조회
 * - 현재 인증 상태 확인
 * - 학습 및 디버깅 지원
 * 
 * 주요 엔드포인트:
 * - GET /debug/filters - 모든 SecurityFilterChain의 필터 목록
 * - GET /debug/chains - 등록된 SecurityFilterChain 정보
 * - GET /debug/current-user - 현재 인증된 사용자 정보
 * - GET /debug/summary - 전체 요약 정보
 * 
 * 사용 예시:
 * curl http://localhost:8080/debug/filters
 * curl http://localhost:8080/debug/chains
 * curl http://localhost:8080/debug/current-user
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    @Autowired(required = false)
    private FilterChainProxy filterChainProxy;
    
    @Autowired(required = false)
    private AuthenticationConfiguration authenticationConfiguration;
    
    @Autowired(required = false)
    private CustomerRepository customerRepository;

    /**
     * 모든 SecurityFilterChain의 필터 목록 반환
     * 
     * 각 체인별로 어떤 필터들이 활성화되어 있는지 확인 가능
     * V1~V5의 필터 차이를 비교할 수 있음
     */
    @GetMapping("/filters")
    public Map<String, Object> getAllFilters() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Spring Security 필터 체인 정보");
        
        if (filterChainProxy == null) {
            response.put("error", "FilterChainProxy를 찾을 수 없습니다");
            return response;
        }

        List<SecurityFilterChain> filterChains = filterChainProxy.getFilterChains();
        List<Map<String, Object>> chainsInfo = new ArrayList<>();
        
        int chainIndex = 1;
        for (SecurityFilterChain chain : filterChains) {
            Map<String, Object> chainInfo = new HashMap<>();
            chainInfo.put("chain", "SecurityFilterChain #" + chainIndex);
            
            // URL 패턴 정보 (DefaultSecurityFilterChain인 경우)
            if (chain instanceof DefaultSecurityFilterChain) {
                DefaultSecurityFilterChain defaultChain = (DefaultSecurityFilterChain) chain;
                chainInfo.put("requestMatcher", defaultChain.getRequestMatcher().toString());
            }
            
            // 필터 목록
            List<String> filterNames = new ArrayList<>();
            for (Filter filter : chain.getFilters()) {
                filterNames.add(filter.getClass().getSimpleName());
            }
            chainInfo.put("filterCount", filterNames.size());
            chainInfo.put("filters", filterNames);
            
            chainsInfo.add(chainInfo);
            chainIndex++;
        }
        
        response.put("totalChains", filterChains.size());
        response.put("chains", chainsInfo);
        response.put("note", "각 체인의 필터 순서와 개수를 확인하세요");
        
        return response;
    }

    /**
     * 등록된 SecurityFilterChain 요약 정보
     * 
     * 각 체인의 URL 패턴과 필터 개수를 한눈에 확인
     */
    @GetMapping("/chains")
    public Map<String, Object> getChainsSummary() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "SecurityFilterChain 요약");
        
        if (filterChainProxy == null) {
            response.put("error", "FilterChainProxy를 찾을 수 없습니다");
            return response;
        }

        List<SecurityFilterChain> filterChains = filterChainProxy.getFilterChains();
        List<Map<String, Object>> chainsSummary = new ArrayList<>();
        
        // 각 체인의 예상 URL 패턴과 특징
        String[] expectedPatterns = {
            "/v1/basic/** (최소 설정)",
            "/v2/formlogin/** (폼 로그인)",
            "/v3/httpbasic/** (HTTP Basic)",
            "/v4/nocsrf/** (CSRF 비활성화)",
            "/v5/api/** (API - Stateless)",
            "/v5/admin/** (Admin - Stateful)",
            "/debug/** (디버그)"
        };
        
        int chainIndex = 0;
        for (SecurityFilterChain chain : filterChains) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("order", chainIndex + 1);
            summary.put("filterCount", chain.getFilters().size());
            
            if (chainIndex < expectedPatterns.length) {
                summary.put("description", expectedPatterns[chainIndex]);
            }
            
            if (chain instanceof DefaultSecurityFilterChain) {
                DefaultSecurityFilterChain defaultChain = (DefaultSecurityFilterChain) chain;
                summary.put("requestMatcher", defaultChain.getRequestMatcher().toString());
            }
            
            chainsSummary.add(summary);
            chainIndex++;
        }
        
        response.put("totalChains", filterChains.size());
        response.put("chainsSummary", chainsSummary);
        response.put("note", "@Order와 securityMatcher로 우선순위와 적용 범위가 결정됩니다");
        
        return response;
    }

    /**
     * 현재 인증된 사용자 정보
     * 
     * SecurityContext에 저장된 Authentication 객체 확인
     */
    @GetMapping("/current-user")
    public Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "현재 인증 정보");
        
        if (auth == null) {
            response.put("authenticated", false);
            response.put("note", "SecurityContext에 Authentication이 없습니다");
            return response;
        }
        
        response.put("authenticated", auth.isAuthenticated());
        response.put("principal", auth.getPrincipal());
        response.put("name", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("authenticationType", auth.getClass().getSimpleName());
        response.put("details", auth.getDetails() != null ? auth.getDetails().toString() : "No details");
        
        return response;
    }

    /**
     * 전체 요약 정보
     * 
     * 모든 디버그 정보를 한 번에 조회
     */
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Spring Security 전체 요약");
        
        // 필터 체인 요약
        if (filterChainProxy != null) {
            List<SecurityFilterChain> filterChains = filterChainProxy.getFilterChains();
            response.put("totalSecurityFilterChains", filterChains.size());
            
            Map<String, Integer> filterCountByChain = new HashMap<>();
            int chainIndex = 1;
            for (SecurityFilterChain chain : filterChains) {
                filterCountByChain.put("Chain #" + chainIndex, chain.getFilters().size());
                chainIndex++;
            }
            response.put("filterCountByChain", filterCountByChain);
        }
        
        // 인증 정보
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put("authenticated", auth != null && auth.isAuthenticated());
        authInfo.put("username", auth != null ? auth.getName() : "N/A");
        authInfo.put("type", auth != null ? auth.getClass().getSimpleName() : "N/A");
        response.put("currentAuthentication", authInfo);
        
        // 학습 가이드
        Map<String, String> guide = new HashMap<>();
        // WEEK 1
        guide.put("V1", "GET /v1/basic/public - 최소 설정 (인증 불가능)");
        guide.put("V2", "GET /v2/formlogin/secured - 폼 로그인");
        guide.put("V3", "curl -u admin:12345 /v3/httpbasic/secured - HTTP Basic");
        guide.put("V4", "curl -X POST -u admin:12345 /v4/nocsrf/create - CSRF 비활성화");
        guide.put("V5_API", "curl -u admin:12345 /v5/api/data - Stateless API");
        guide.put("V5_Admin", "브라우저로 /v5/admin/dashboard - Stateful 관리자");
        // WEEK 2
        guide.put("V6", "curl -u admin:admin123 /v6/jdbc/secured - JDBC 스타일");
        guide.put("V7", "curl -u admin@example.com:admin123 /v7/custom/admin - Custom UserDetailsService");
        guide.put("V8", "POST /v8/register/signup (회원가입) → curl -u email:pwd /v8/register/myinfo");
        guide.put("V9", "curl -u admin@example.com:admin123 /v9/authprovider/business-check - Custom AuthenticationProvider");
        guide.put("V10", "브라우저로 /v10/failure/secured 접근 후 잘못된 비밀번호 입력");
        response.put("testGuide", guide);
        
        // 필터 비교
        Map<String, String> filterComparison = new HashMap<>();
        // WEEK 1
        filterComparison.put("V1", "~10개 필터 (인증 필터 없음)");
        filterComparison.put("V2", "~13개 필터 (UsernamePasswordAuthenticationFilter 추가)");
        filterComparison.put("V3", "~11개 필터 (BasicAuthenticationFilter 추가)");
        filterComparison.put("V4", "~10개 필터 (CsrfFilter 제거, STATELESS)");
        filterComparison.put("V5_API", "~10개 필터 (BasicAuthenticationFilter, STATELESS)");
        filterComparison.put("V5_Admin", "~13개 필터 (UsernamePasswordAuthenticationFilter, Stateful)");
        // WEEK 2
        filterComparison.put("V6", "~11개 필터 (DaoAuthenticationProvider + JDBC 스타일)");
        filterComparison.put("V7", "~11개 필터 (DaoAuthenticationProvider + Custom UserDetailsService)");
        filterComparison.put("V8", "~11개 필터 (회원가입 API + PasswordEncoder)");
        filterComparison.put("V9", "~11개 필터 (CustomAuthenticationProvider - 비즈니스 로직 포함)");
        filterComparison.put("V10", "~13개 필터 (FormLogin + Custom FailureHandler)");
        response.put("filterComparison", filterComparison);
        
        return response;
    }

    /**
     * WEEK 2: 등록된 AuthenticationProvider 목록 조회
     * 
     * 학습 목표:
     * - UserDetailsService 방식과 AuthenticationProvider 방식의 차이 확인
     * - DaoAuthenticationProvider vs CustomAuthenticationProvider
     * 
     * 예상 결과:
     * - V6, V7, V8, V10: DaoAuthenticationProvider (UserDetailsService 사용)
     * - V9: CustomAuthenticationProvider (직접 구현)
     */
    @GetMapping("/authentication-providers")
    public Map<String, Object> getAuthenticationProviders() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "등록된 AuthenticationProvider 목록");
        
        if (authenticationConfiguration == null) {
            response.put("error", "AuthenticationConfiguration을 찾을 수 없습니다");
            return response;
        }
        
        try {
            AuthenticationManager authManager = authenticationConfiguration.getAuthenticationManager();
            
            if (authManager instanceof ProviderManager) {
                ProviderManager providerManager = (ProviderManager) authManager;
                List<AuthenticationProvider> providers = providerManager.getProviders();
                
                List<Map<String, Object>> providerInfoList = new ArrayList<>();
                for (AuthenticationProvider provider : providers) {
                    Map<String, Object> providerInfo = new HashMap<>();
                    providerInfo.put("className", provider.getClass().getName());
                    providerInfo.put("simpleName", provider.getClass().getSimpleName());
                    providerInfoList.add(providerInfo);
                }
                
                response.put("totalProviders", providers.size());
                response.put("providers", providerInfoList);
                
                // 학습 노트
                Map<String, String> learningNote = new HashMap<>();
                learningNote.put("DaoAuthenticationProvider", "UserDetailsService + PasswordEncoder 자동 사용");
                learningNote.put("CustomAuthenticationProvider", "모든 인증 로직 직접 구현 (V9에서 사용)");
                learningNote.put("중요", "AuthenticationProvider Bean 등록 시 DaoAuthenticationProvider 생성 안 됨");
                response.put("learningNote", learningNote);
                
            } else {
                response.put("authenticationManagerType", authManager.getClass().getSimpleName());
                response.put("note", "ProviderManager가 아닌 다른 타입입니다");
            }
            
        } catch (Exception e) {
            response.put("error", "AuthenticationManager 조회 실패: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * WEEK 2: 인메모리 Repository에 저장된 모든 Customer 조회
     * 
     * 학습 목표:
     * - 회원가입으로 등록된 사용자 확인
     * - 비밀번호가 BCrypt로 해싱되었는지 확인
     * - Repository 패턴 동작 확인
     * 
     * 보안:
     * - 비밀번호는 일부만 표시 (마스킹)
     * - 실제 운영 환경에서는 이런 엔드포인트를 제공하면 안 됨
     */
    @GetMapping("/customers")
    public Map<String, Object> getAllCustomers() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "인메모리 Customer Repository 조회");
        
        if (customerRepository == null) {
            response.put("error", "CustomerRepository를 찾을 수 없습니다");
            response.put("note", "WEEK 2 구현이 필요합니다");
            return response;
        }
        
        List<Customer> customers = customerRepository.findAll();
        
        List<Map<String, Object>> customerInfoList = customers.stream()
                .map(customer -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", customer.getId());
                    info.put("email", customer.getEmail());
                    // 비밀번호는 앞 부분만 표시 (보안)
                    String pwd = customer.getPwd();
                    info.put("passwordHash", pwd != null && pwd.length() > 20 
                            ? pwd.substring(0, 20) + "..." 
                            : "N/A");
                    info.put("passwordHashLength", pwd != null ? pwd.length() : 0);
                    info.put("isBCryptHash", pwd != null && (pwd.startsWith("$2a$") || pwd.startsWith("$2b$")));
                    info.put("role", customer.getRole());
                    info.put("name", customer.getName());
                    return info;
                })
                .collect(Collectors.toList());
        
        response.put("totalCustomers", customers.size());
        response.put("customers", customerInfoList);
        
        // 학습 노트
        Map<String, String> learningNote = new HashMap<>();
        learningNote.put("BCrypt Hash", "$2a$ 또는 $2b$로 시작, 약 60자 길이");
        learningNote.put("회원가입", "POST /v8/register/signup 으로 새 사용자 등록 가능");
        learningNote.put("로그인", "등록한 이메일/비밀번호로 HTTP Basic 인증 가능");
        response.put("learningNote", learningNote);
        
        return response;
    }
    
    /**
     * 필터 체인 실행 순서 가이드
     */
    @GetMapping("/filter-order-guide")
    public Map<String, Object> getFilterOrderGuide() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Spring Security 표준 필터 순서");
        
        Map<Integer, String> filterOrder = new HashMap<>();
        filterOrder.put(1, "DisableEncodingFilter");
        filterOrder.put(2, "WebAsyncManagerIntegrationFilter");
        filterOrder.put(3, "SecurityContextHolderFilter - SecurityContext 로드");
        filterOrder.put(4, "HeaderWriterFilter - 보안 헤더 추가");
        filterOrder.put(5, "CorsFilter - CORS 처리");
        filterOrder.put(6, "CsrfFilter - CSRF 토큰 검증");
        filterOrder.put(7, "LogoutFilter - 로그아웃 처리");
        filterOrder.put(9, "UsernamePasswordAuthenticationFilter - 폼 로그인");
        filterOrder.put(10, "DefaultLoginPageGeneratingFilter - 기본 로그인 페이지");
        filterOrder.put(12, "BasicAuthenticationFilter - HTTP Basic 인증");
        filterOrder.put(14, "RequestCacheAwareFilter - 요청 URL 복원");
        filterOrder.put(15, "SecurityContextHolderAwareRequestFilter");
        filterOrder.put(16, "AnonymousAuthenticationFilter - 익명 사용자");
        filterOrder.put(17, "SessionManagementFilter - 세션 관리");
        filterOrder.put(18, "ExceptionTranslationFilter - 예외 처리");
        filterOrder.put(19, "AuthorizationFilter - 권한 검사");
        
        response.put("standardFilterOrder", filterOrder);
        response.put("note", "설정에 따라 일부 필터는 활성화되지 않거나 제거될 수 있습니다");
        
        Map<String, String> configToFilter = new HashMap<>();
        configToFilter.put(".formLogin()", "UsernamePasswordAuthenticationFilter 추가");
        configToFilter.put(".httpBasic()", "BasicAuthenticationFilter 추가");
        configToFilter.put(".csrf().disable()", "CsrfFilter 제거");
        configToFilter.put(".oauth2Login()", "OAuth2 관련 필터 추가");
        configToFilter.put("SessionCreationPolicy.STATELESS", "세션 생성 안 함");
        
        response.put("configToFilterMapping", configToFilter);
        
        return response;
    }
}

