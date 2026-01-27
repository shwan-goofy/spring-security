# Spring Security Filter Chain 학습 프로젝트

Spring Security의 필터 체인 동작을 단계별로 이해하기 위한 실습 프로젝트입니다.

## 빠른 시작

### 1. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 2. 기본 테스트

```bash
# V1 - 공개 엔드포인트
curl http://localhost:8080/v1/basic/public

# V3 - HTTP Basic 인증
curl --user admin:12345 http://localhost:8080/v3/httpbasic/secured

# V4 - POST 요청 (CSRF 토큰 없이)
curl --request POST \
  --user admin:12345 \
  --header "Content-Type: application/json" \
  --data '{"data":"test"}' \
  http://localhost:8080/v4/nocsrf/create

# Debug - 필터 체인 확인
curl http://localhost:8080/debug/summary
```

### 3. 브라우저 테스트

- V2 폼 로그인: http://localhost:8080/v2/formlogin/secured
- V5 관리자 페이지: http://localhost:8080/v5/admin/dashboard

**로그인 계정**:
- admin / 12345 (모든 권한)
- user / 12345 (일반 사용자, Admin 페이지 접근 불가)

## 프로젝트 구조

```
hs-spring-security/
├── src/main/java/.../
│   ├── common/              # 공통 설정 (PasswordEncoder, UserDetailsService)
│   ├── week1/
│   │   ├── v1basic/         # V1 - 최소 설정 (인증 필터 없음)
│   │   ├── v2formlogin/     # V2 - 폼 로그인
│   │   ├── v3httpbasic/     # V3 - HTTP Basic 인증
│   │   ├── v4nocsrf/        # V4 - CSRF 비활성화 + STATELESS
│   │   └── v5multichain/    # V5 - 다중 SecurityFilterChain (API + Admin)
│   └── debug/               # Debug 엔드포인트 (필터 체인 정보 조회)
├── TEST_GUIDE.md            # 상세 테스트 가이드
├── IMPLEMENTATION_SUMMARY.md # 구현 완료 요약
└── README.md                # 이 파일
```

## 7개의 SecurityFilterChain

| Order | URL 패턴 | 인증 방식 | CSRF | 세션 | 특징 |
|-------|----------|----------|------|------|------|
| 1 | /v1/basic/** | 없음 | ✓ | Stateful | 인증 불가능 (403) |
| 2 | /v2/formlogin/** | 폼 로그인 | ✓ | Stateful | 브라우저 로그인 페이지 |
| 3 | /v3/httpbasic/** | HTTP Basic | ✓ | Stateful | curl 테스트 용이 |
| 4 | /v4/nocsrf/** | HTTP Basic | ✗ | STATELESS | REST API 스타일 |
| 5 | /v5/api/** | HTTP Basic | ✗ | STATELESS | API 전용 |
| 6 | /v5/admin/** | 폼 로그인 | ✓ | Stateful | Admin 권한 필요 |
| 99 | /debug/** | 없음 | ✓ | Stateful | 디버그 정보 조회 |

## 주요 학습 포인트

### 1. SecurityFilterChain DSL과 필터 매핑

```java
// formLogin() → UsernamePasswordAuthenticationFilter 추가
.formLogin(withDefaults())

// httpBasic() → BasicAuthenticationFilter 추가
.httpBasic(withDefaults())

// CSRF 비활성화 → CsrfFilter 제거
.csrf(csrf -> csrf.disable())

// STATELESS → 세션 생성 안 함
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

### 2. 다중 SecurityFilterChain

```java
@Bean @Order(5)
SecurityFilterChain apiChain(HttpSecurity http) {
    http.securityMatcher("/v5/api/**")  // API 전용
        .csrf(csrf -> csrf.disable())
        ...
}

@Bean @Order(6)
SecurityFilterChain adminChain(HttpSecurity http) {
    http.securityMatcher("/v5/admin/**")  // Admin 전용
        .authorizeHttpRequests(auth -> auth
            .anyRequest().hasRole("ADMIN"))
        ...
}
```

## 디버그 엔드포인트

```bash
# 모든 SecurityFilterChain의 필터 목록
curl http://localhost:8080/debug/filters

# SecurityFilterChain 요약
curl http://localhost:8080/debug/chains

# 현재 인증 사용자
curl http://localhost:8080/debug/current-user

# 전체 요약
curl http://localhost:8080/debug/summary

# 필터 순서 가이드
curl http://localhost:8080/debug/filter-order-guide
```

## 로그 확인

애플리케이션 시작 시 콘솔에서 다음 정보 확인 가능:

```
DEBUG o.s.s.web.DefaultSecurityFilterChain : Will secure Or [PathPattern [/v1/basic/**]] with filters: DisableEncodeUrlFilter, WebAsyncManagerIntegrationFilter, SecurityContextHolderFilter, ...
```

각 요청마다 필터 실행 과정 로그:

```
DEBUG o.s.security.web.FilterChainProxy : Securing GET /v3/httpbasic/secured
DEBUG o.s.security.web.FilterChainProxy : /v3/httpbasic/secured at position 1 of 12 in additional filter chain; firing Filter: 'DisableEncodeUrlFilter'
...
```

## 상세 문서

- **TEST_GUIDE.md**: 각 버전별 상세 테스트 시나리오 및 예상 결과
- **IMPLEMENTATION_SUMMARY.md**: 구현 완료 요약 및 검증 결과
- **WEEK 1/README.md**: Spring Security 이론 및 아키텍처 설명

## 기술 스택

- Spring Boot 4.0.2
- Spring Security 7.x
- Java 17
- Gradle 9.3.0

## 문제 해결

### 포트 8080이 이미 사용 중

```bash
# 프로세스 확인 및 종료
lsof -ti:8080 | xargs kill -9
```

### 로그인 실패 (401 Unauthorized)

- 테스트 계정: admin/12345 또는 user/12345
- 비밀번호는 BCrypt로 암호화됨

### CSRF 토큰 오류 (POST 요청)

- V2, V3, V5 Admin: CSRF 활성화 → 브라우저 사용 권장
- V4, V5 API: CSRF 비활성화 → curl/Postman 사용 가능

## 다음 단계

1. `TEST_GUIDE.md`의 모든 테스트 시나리오 수행
2. 각 버전별 로그 확인 및 필터 차이 분석
3. 브라우저 개발자 도구로 네트워크 요청 확인
4. WEEK 2 학습으로 진행 (커스텀 인증 로직)

## 라이센스

교육 목적의 학습 프로젝트

---

**Created**: 2026-01-27  
**Based on**: WEEK 1 Spring Security 기본 개념과 인증 흐름

