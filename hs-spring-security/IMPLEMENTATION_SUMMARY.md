# Spring Security Filter Chain 학습 프로젝트 - 구현 완료 요약

## 프로젝트 개요

이 프로젝트는 Spring Security의 필터 체인 동작을 단계별로 이해하기 위한 학습 프로젝트입니다. WEEK 1 수업 자료를 기반으로 V1~V5의 버전별 SecurityFilterChain을 구현하여, 각 설정에 따른 필터 차이를 직접 확인할 수 있습니다.

## 구현된 구조

### 패키지 구조

```
src/main/java/org/example/hsspringsecurity/
├── HsSpringSecurityApplication.java           # 메인 애플리케이션
├── common/                                     # 공통 설정
│   ├── PasswordEncoderConfig.java            # BCryptPasswordEncoder Bean
│   └── UserDetailsServiceConfig.java         # 테스트 사용자 (admin, user)
├── week1/                                      # WEEK 1 학습 내용
│   ├── v1basic/                               # V1 - 최소 설정
│   │   ├── BasicSecurityConfig.java          # @Order(1), 인증 필터 없음
│   │   └── BasicController.java              # 4개 엔드포인트
│   ├── v2formlogin/                           # V2 - 폼 로그인
│   │   ├── FormLoginSecurityConfig.java      # @Order(2), formLogin()
│   │   └── FormLoginController.java          # 5개 엔드포인트
│   ├── v3httpbasic/                           # V3 - HTTP Basic
│   │   ├── HttpBasicSecurityConfig.java      # @Order(3), httpBasic()
│   │   └── HttpBasicController.java          # 6개 엔드포인트
│   ├── v4nocsrf/                              # V4 - CSRF 비활성화
│   │   ├── NoCsrfSecurityConfig.java         # @Order(4), csrf().disable()
│   │   └── NoCsrfController.java             # 7개 엔드포인트
│   └── v5multichain/                          # V5 - 다중 체인
│       ├── MultiChainSecurityConfig.java      # @Order(5,6), 2개 체인
│       ├── ApiController.java                 # /v5/api/** (Stateless)
│       └── AdminController.java               # /v5/admin/** (Stateful)
└── debug/                                      # 디버그 엔드포인트
    ├── DebugSecurityConfig.java               # @Order(99), permitAll
    └── DebugController.java                   # 6개 디버그 엔드포인트
```

## 구현된 SecurityFilterChain

총 7개의 SecurityFilterChain이 등록되어 있으며, 각각 다른 URL 패턴과 보안 설정을 가집니다:

| Order | URL 패턴 | 인증 방식 | CSRF | 세션 | 필터 개수 | 주요 특징 |
|-------|----------|----------|------|------|----------|----------|
| 1 | /v1/basic/** | 없음 | ✓ | Stateful | 11개 | 인증 필터 없음 (로그인 불가능) |
| 2 | /v2/formlogin/** | 폼 로그인 | ✓ | Stateful | 15개 | UsernamePasswordAuthenticationFilter 추가 |
| 3 | /v3/httpbasic/** | HTTP Basic | ✓ | Stateful | 12개 | BasicAuthenticationFilter 추가 |
| 4 | /v4/nocsrf/** | HTTP Basic | ✗ | STATELESS | 12개 | CsrfFilter 제거, STATELESS 모드 |
| 5 | /v5/api/** | HTTP Basic | ✗ | STATELESS | 12개 | REST API용 (Stateless) |
| 6 | /v5/admin/** | 폼 로그인 | ✓ | Stateful | 15개 | Admin 전용 (hasRole("ADMIN")) |
| 99 | /debug/** | 없음 | ✓ | Stateful | 11개 | 디버그용 (permitAll) |

## 주요 학습 포인트

### 1. Bean-Filter 자동 매핑

```java
// PasswordEncoder + UserDetailsService Bean 등록
@Bean PasswordEncoder passwordEncoder() { ... }
@Bean UserDetailsService userDetailsService(PasswordEncoder encoder) { ... }

// ↓ Spring Security가 자동으로 수행
// DaoAuthenticationProvider 생성 및 등록
// AuthenticationManager에 Provider 등록
// 인증 필터들이 이 AuthenticationManager 사용
```

### 2. SecurityFilterChain DSL과 필터 매핑

| DSL 설정 | 추가되는 필터 | 제거되는 필터 |
|----------|-------------|-------------|
| `.formLogin()` | UsernamePasswordAuthenticationFilter, DefaultLoginPageGeneratingFilter | - |
| `.httpBasic()` | BasicAuthenticationFilter | - |
| `.csrf().disable()` | - | CsrfFilter |
| `.sessionManagement(STATELESS)` | - | SessionManagementFilter (비활성화) |

### 3. 다중 SecurityFilterChain 전략

```java
@Bean @Order(5)
SecurityFilterChain apiChain(HttpSecurity http) {
    http.securityMatcher("/v5/api/**")  // URL 패턴 지정
        .csrf(csrf -> csrf.disable())    // API용 설정
        ...
}

@Bean @Order(6)
SecurityFilterChain adminChain(HttpSecurity http) {
    http.securityMatcher("/v5/admin/**")  // 다른 URL 패턴
        .authorizeHttpRequests(auth -> auth
            .anyRequest().hasRole("ADMIN")  // Admin 전용
        )
        ...
}
```

**FilterChainProxy가 요청마다 RequestMatcher로 적절한 체인 선택**

### 4. 필터 실행 순서

표준 필터 순서 (Spring Security 6.x ~ 7.x):

1. DisableEncodeUrlFilter
2. WebAsyncManagerIntegrationFilter
3. **SecurityContextHolderFilter** ← 세션에서 SecurityContext 로드
4. HeaderWriterFilter
5. CorsFilter (설정 시)
6. **CsrfFilter** ← CSRF 토큰 검증 (disable 가능)
7. **LogoutFilter** ← 로그아웃 처리
8. OAuth2AuthorizationRequestRedirectFilter (설정 시)
9. **UsernamePasswordAuthenticationFilter** ← 폼 로그인 (formLogin 설정 시)
10. DefaultLoginPageGeneratingFilter (formLogin 설정 시)
11. DefaultLogoutPageGeneratingFilter (formLogin 설정 시)
12. **BasicAuthenticationFilter** ← HTTP Basic 인증 (httpBasic 설정 시)
13. OAuth2LoginAuthenticationFilter (설정 시)
14. RequestCacheAwareFilter
15. SecurityContextHolderAwareRequestFilter
16. RememberMeAuthenticationFilter (설정 시)
17. **AnonymousAuthenticationFilter** ← 익명 사용자 권한 부여
18. SessionManagementFilter
19. **ExceptionTranslationFilter** ← 인증/인가 예외 처리
20. **AuthorizationFilter** ← URL 기반 권한 검사

## 테스트 계정

| Username | Password | Roles | 비고 |
|----------|----------|-------|------|
| admin | 12345 | ROLE_ADMIN, ROLE_USER | /v5/admin 접근 가능 |
| user | 12345 | ROLE_USER | /v5/admin 접근 불가 (403) |

비밀번호는 BCryptPasswordEncoder로 암호화되어 저장됩니다.

## 주요 테스트 시나리오

### V1 - 최소 설정
```bash
# 공개 엔드포인트 (성공)
curl http://localhost:8080/v1/basic/public
→ 200 OK

# 보호된 엔드포인트 (실패)
curl http://localhost:8080/v1/basic/secured
→ 403 Forbidden (인증 방법 없음)
```

### V2 - 폼 로그인
```bash
# 브라우저로 접근
http://localhost:8080/v2/formlogin/secured
→ 302 Redirect to /login

# 로그인 후 재접근
→ 200 OK (세션 유지)
```

### V3 - HTTP Basic
```bash
# HTTP Basic 인증
curl -u admin:12345 http://localhost:8080/v3/httpbasic/secured
→ 200 OK

# 인증 없이 접근
curl http://localhost:8080/v3/httpbasic/secured
→ 401 Unauthorized
```

### V4 - CSRF 비활성화
```bash
# POST 요청 (CSRF 토큰 없이)
curl -X POST -u admin:12345 \
  -H "Content-Type: application/json" \
  -d '{"data":"test"}' \
  http://localhost:8080/v4/nocsrf/create
→ 200 OK (V2, V3와 달리 성공!)
```

### V5 - 다중 체인
```bash
# API 엔드포인트 (Stateless)
curl -u admin:12345 http://localhost:8080/v5/api/data
→ 200 OK

# Admin 엔드포인트 (Stateful, 브라우저)
http://localhost:8080/v5/admin/dashboard
→ admin 로그인: 200 OK
→ user 로그인: 403 Forbidden (ROLE_ADMIN 없음)
```

### Debug 엔드포인트
```bash
# 모든 필터 체인 정보
curl http://localhost:8080/debug/filters

# 요약 정보
curl http://localhost:8080/debug/summary

# 현재 인증 사용자
curl http://localhost:8080/debug/current-user
```

## 로그 확인

`application.properties`에 다음 설정이 추가되어 상세 로그가 출력됩니다:

```properties
logging.level.org.springframework.security.web.FilterChainProxy=DEBUG
logging.level.org.springframework.security=DEBUG
```

애플리케이션 시작 시 각 SecurityFilterChain의 필터 목록이 출력됩니다:

```
DEBUG o.s.s.web.DefaultSecurityFilterChain : Will secure Or [PathPattern [/v1/basic/**]] with filters: DisableEncodeUrlFilter, WebAsyncManagerIntegrationFilter, SecurityContextHolderFilter, ...
```

각 요청마다 실행되는 필터와 인증 과정이 로그로 출력됩니다:

```
DEBUG o.s.security.web.FilterChainProxy : Securing GET /v3/httpbasic/secured
DEBUG o.s.security.web.FilterChainProxy : /v3/httpbasic/secured at position 1 of 12 in additional filter chain; firing Filter: 'DisableEncodeUrlFilter'
...
DEBUG o.s.s.w.a.www.BasicAuthenticationFilter : Set SecurityContextHolder to UsernamePasswordAuthenticationToken [Principal=admin, ...]
```

## 검증 완료 항목

✅ 빌드 성공 (./gradlew build)
✅ 애플리케이션 시작 성공
✅ 7개의 SecurityFilterChain 등록 확인
✅ V1 공개 엔드포인트 접근 성공 (200 OK)
✅ V1 보호된 엔드포인트 접근 실패 (403 Forbidden)
✅ V3 HTTP Basic 인증 성공 (admin:12345)
✅ V4 POST 요청 CSRF 토큰 없이 성공
✅ V5 API 엔드포인트 접근 성공
✅ Debug 엔드포인트로 필터 체인 정보 조회 성공
✅ 로그에서 필터 체인 실행 과정 확인

## 다음 단계

1. **실습**: `TEST_GUIDE.md` 문서를 참고하여 모든 테스트 시나리오 수행
2. **비교**: 각 버전별 로그를 확인하여 필터 차이 분석
3. **확장**: 
   - 커스텀 필터 추가 (WEEK 2)
   - 권한 기반 접근 제어 (WEEK 3)
   - JWT 인증 구현 (WEEK 4+)
4. **학습 체크리스트**: `TEST_GUIDE.md`의 학습 체크리스트 완료

## 참고 자료

- WEEK 1/README.md: Spring Security 기본 개념과 인증 흐름
- TEST_GUIDE.md: 상세한 테스트 가이드 및 예상 결과
- Spring Security 공식 문서: https://docs.spring.io/spring-security/reference/

---

**구현 완료일**: 2026-01-27
**Spring Boot 버전**: 4.0.2
**Spring Security 버전**: 7.x (Spring Boot 4.0.2에 포함)
**Java 버전**: 17

