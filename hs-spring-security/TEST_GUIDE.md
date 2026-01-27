# Spring Security Filter Chain 학습 프로젝트 테스트 가이드

이 문서는 V1~V10 각 버전의 엔드포인트를 테스트하고 필터 체인의 차이를 확인하는 가이드입니다.

- **WEEK 1 (V1~V5)**: 필터 체인 기본 동작 이해
- **WEEK 2 (V6~V10)**: DB 연동 인증과 비밀번호 암호화

## 사전 준비

### 1. 애플리케이션 실행

```bash
cd hs-spring-security
./gradlew bootRun
```

또는 IDE에서 `HsSpringSecurityApplication` 실행

### 2. 테스트 도구

- **브라우저**: Chrome, Firefox 등
- **curl**: 터미널에서 실행
- **Postman**: REST API 테스트 도구

### 3. 테스트 계정

**WEEK 1 (V1~V5)**:

| Username | Password | Roles |
|----------|----------|-------|
| admin | 12345 | ROLE_ADMIN, ROLE_USER |
| user | 12345 | ROLE_USER |

**WEEK 2 (V6~V10)**:

| Email | Password | Role |
|-------|----------|------|
| admin@example.com | admin123 | ROLE_ADMIN |
| user@example.com | user123 | ROLE_USER |

**참고**: WEEK 2에서는 이메일을 username으로 사용하며, 비밀번호가 BCrypt로 해싱되어 저장됩니다.

---

# WEEK 1: 필터 체인 기본 동작

---

## V1 - 최소 설정 테스트

**목적**: 기본 필터 체인 구조 이해, 인증 방법이 없을 때의 동작 확인

### 예상 필터 (약 10개)
- SecurityContextHolderFilter
- HeaderWriterFilter
- CsrfFilter
- LogoutFilter
- RequestCacheAwareFilter
- AnonymousAuthenticationFilter
- SessionManagementFilter
- ExceptionTranslationFilter
- AuthorizationFilter

### 테스트 시나리오

#### 1. 공개 엔드포인트 (인증 불필요)
```bash
curl http://localhost:8080/v1/basic/public
```
**예상 결과**: 200 OK

#### 2. 보호된 엔드포인트 (인증 필요하지만 인증 방법 없음)
```bash
curl http://localhost:8080/v1/basic/secured
```
**예상 결과**: 403 Forbidden
**이유**: formLogin(), httpBasic()이 없어서 인증 불가능

#### 3. 브라우저 테스트
```
http://localhost:8080/v1/basic/secured
```
**예상 결과**: 403 Forbidden (로그인 페이지로 리디렉션 안 됨)

### 학습 포인트
- UsernamePasswordAuthenticationFilter 없음 (formLogin 미설정)
- BasicAuthenticationFilter 없음 (httpBasic 미설정)
- 따라서 인증 방법이 없어서 로그인 불가능
- AnonymousAuthenticationFilter가 익명 권한 부여하지만, authenticated() 요구로 접근 거부

---

## V2 - 폼 로그인 테스트

**목적**: formLogin() 설정 시 UsernamePasswordAuthenticationFilter 추가 확인

### 추가 필터
- UsernamePasswordAuthenticationFilter (순서 9)
- DefaultLoginPageGeneratingFilter (순서 10)
- DefaultLogoutPageGeneratingFilter (순서 11)

### 테스트 시나리오

#### 1. 공개 엔드포인트
```bash
curl http://localhost:8080/v2/formlogin/public
```
**예상 결과**: 200 OK

#### 2. 브라우저로 보호된 엔드포인트 접근
```
http://localhost:8080/v2/formlogin/secured
```
**예상 결과**: 302 Redirect to /login

#### 3. 로그인 페이지 확인
```
http://localhost:8080/login
```
**예상 결과**: 기본 로그인 페이지 표시 (DefaultLoginPageGeneratingFilter)

#### 4. 로그인 (브라우저)
- Username: `admin`
- Password: `12345`

**예상 결과**: 로그인 성공 후 /v2/formlogin/secured로 리디렉션

#### 5. 세션 유지 확인
로그인 후 다시 접근:
```
http://localhost:8080/v2/formlogin/user
```
**예상 결과**: 200 OK (재인증 불필요, 세션에서 SecurityContext 로드)

#### 6. 세션 정보 확인
```
http://localhost:8080/v2/formlogin/session-info
```
**예상 결과**: 세션 기반 인증 정보 표시

### 학습 포인트
- UsernamePasswordAuthenticationFilter가 POST /login 처리
- SecurityContextHolderFilter가 세션에서 SecurityContext 로드
- RequestCache가 인증 전 URL 저장했다가 로그인 후 복원
- 세션에 SecurityContext 저장되어 재인증 불필요

---

## V3 - HTTP Basic 인증 테스트

**목적**: httpBasic() 설정 시 BasicAuthenticationFilter 추가 확인

### 추가 필터
- BasicAuthenticationFilter (순서 12)

### 테스트 시나리오

#### 1. curl로 인증 없이 접근
```bash
curl http://localhost:8080/v3/httpbasic/secured
```
**예상 결과**: 401 Unauthorized
**응답 헤더**: `WWW-Authenticate: Basic realm="Realm"`

#### 2. curl로 HTTP Basic 인증
```bash
curl --user admin:12345 http://localhost:8080/v3/httpbasic/secured
```
**예상 결과**: 200 OK + 사용자 정보

**참고**: `-u`는 `--user`의 축약형입니다.

#### 3. user 계정으로 접근
```bash
curl --user user:12345 http://localhost:8080/v3/httpbasic/user
```
**예상 결과**: 200 OK

#### 4. 브라우저 테스트
```
http://localhost:8080/v3/httpbasic/secured
```
**예상 결과**: 브라우저 로그인 팝업 표시

#### 5. 인증 정보 확인
```bash
curl --user admin:12345 http://localhost:8080/v3/httpbasic/auth-info
```
**예상 결과**: HTTP Basic 인증 정보 표시

#### 6. Authorization 헤더 직접 지정
```bash
# Base64 인코딩: echo -n "admin:12345" | base64
# 결과: YWRtaW46MTIzNDU=
curl -H "Authorization: Basic YWRtaW46MTIzNDU=" http://localhost:8080/v3/httpbasic/secured
```
**예상 결과**: 200 OK

### 학습 포인트
- BasicAuthenticationFilter가 Authorization: Basic 헤더 처리
- 매 요청마다 Authorization 헤더에 자격 증명 포함
- 브라우저는 자격 증명을 캐시하여 자동으로 헤더 추가
- 세션에도 저장되지만 매번 헤더로도 인증 가능

---

## V4 - CSRF 비활성화 테스트

**목적**: CSRF 비활성화 시 CsrfFilter 제거 확인, Stateless 이해

### 제거된 필터
- CsrfFilter (순서 6)

### 변경된 설정
- SessionCreationPolicy.STATELESS

### 테스트 시나리오

#### 1. GET 요청
```bash
curl --user admin:12345 http://localhost:8080/v4/nocsrf/secured
```
**예상 결과**: 200 OK

#### 2. POST 요청 (CSRF 토큰 없이)
```bash
curl --request POST \
  --user admin:12345 \
  --header "Content-Type: application/json" \
  --data '{"data":"test"}' \
  http://localhost:8080/v4/nocsrf/create
```
**예상 결과**: 200 OK (V2, V3와 달리 CSRF 토큰 불필요!)

**참고**: 
- `-X POST` = `--request POST`
- `-u` = `--user`
- `-H` = `--header`
- `-d` = `--data`

#### 3. PUT 요청
```bash
curl --request PUT \
  --user admin:12345 \
  --header "Content-Type: application/json" \
  --data '{"id":1,"data":"updated"}' \
  http://localhost:8080/v4/nocsrf/update
```
**예상 결과**: 200 OK

#### 4. DELETE 요청
```bash
curl --request DELETE \
  --user admin:12345 \
  http://localhost:8080/v4/nocsrf/delete
```
**예상 결과**: 200 OK

#### 5. 세션 확인 (헤더 포함)
```bash
curl --include --user admin:12345 http://localhost:8080/v4/nocsrf/secured
```
**예상 결과**: Set-Cookie 헤더 없음 (STATELESS)

**참고**: `-i` = `--include` (응답 헤더 포함)

#### 6. Stateless 확인
```bash
curl --user admin:12345 http://localhost:8080/v4/nocsrf/stateless-check
```
**예상 결과**: Stateless 정보 표시

#### 7. 필터 비교
```bash
curl --user admin:12345 http://localhost:8080/v4/nocsrf/filter-comparison
```
**예상 결과**: V2/V3와 V4의 필터 차이 표시

### 학습 포인트
- CsrfFilter 제거로 POST 요청 시 CSRF 토큰 불필요
- STATELESS 모드로 세션 생성 안 함
- 매 요청마다 Authorization 헤더 필요
- REST API, 마이크로서비스에 적합

---

## V5 - 다중 SecurityFilterChain 테스트

**목적**: URL 패턴별로 다른 보안 전략 적용 확인

### 2개의 SecurityFilterChain
1. **apiSecurityFilterChain** (Order 5): /v5/api/** - Stateless
2. **adminSecurityFilterChain** (Order 6): /v5/admin/** - Stateful

### API 체인 테스트 (/v5/api/**)

#### 1. 공개 API
```bash
curl http://localhost:8080/v5/api/public
```
**예상 결과**: 200 OK

#### 2. 보호된 API (GET)
```bash
curl --user admin:12345 http://localhost:8080/v5/api/data
```
**예상 결과**: 200 OK

#### 3. POST 요청 (CSRF 토큰 없이)
```bash
curl --request POST \
  --user admin:12345 \
  --header "Content-Type: application/json" \
  --data '{"data":"test"}' \
  http://localhost:8080/v5/api/data
```
**예상 결과**: 200 OK

#### 4. API 설정 확인
```bash
curl --user admin:12345 http://localhost:8080/v5/api/config
```
**예상 결과**: API SecurityFilterChain 설정 정보

### Admin 체인 테스트 (/v5/admin/**)

#### 1. 브라우저로 접근 (미인증)
```
http://localhost:8080/v5/admin/dashboard
```
**예상 결과**: 302 Redirect to /login

#### 2. admin 계정으로 로그인
- Username: `admin`
- Password: `12345`

**예상 결과**: 로그인 성공 후 /v5/admin/dashboard로 리디렉션

#### 3. user 계정으로 로그인 시도
- Username: `user`
- Password: `12345`

**예상 결과**: 로그인 성공하지만 /v5/admin/dashboard 접근 시 **403 Forbidden**
**이유**: user는 ROLE_USER만 가지고 있고, ROLE_ADMIN이 필요

#### 4. admin으로 로그인 후 인증 정보 확인
```
http://localhost:8080/v5/admin/auth-info
```
**예상 결과**: Admin 체인의 상세 정보 표시

#### 5. 비교 정보 확인
```
http://localhost:8080/v5/admin/comparison
```
**예상 결과**: API 체인과 Admin 체인의 차이점 표시

### 학습 포인트
- 하나의 애플리케이션에서 여러 보안 전략 동시 운영
- @Order로 우선순위 제어
- securityMatcher로 적용 범위 지정
- API: Stateless + HTTP Basic + CSRF 비활성화
- Admin: Stateful + 폼 로그인 + CSRF 활성화 + ROLE_ADMIN 권한

---

# WEEK 2: DB 연동 인증과 비밀번호 암호화

## V6 - JDBC 스타일 UserDetailsService 테스트

**목적**: Spring Security 표준 스키마(users, authorities) 시뮬레이션, JdbcUserDetailsManager 동작 이해

### 특징
- JDBC 표준 스키마 (`users`, `authorities` 테이블) 인메모리 시뮬레이션
- JdbcStyleUserDetailsService 구현
- DaoAuthenticationProvider 자동 생성

### 테스트 시나리오

#### 1. 공개 엔드포인트
```bash
curl http://localhost:8080/v6/jdbc/public
```
**예상 결과**: 200 OK

#### 2. JDBC 스타일 인증
```bash
curl --user admin:admin123 http://localhost:8080/v6/jdbc/secured
```
**예상 결과**: 200 OK (users 테이블에서 조회)

#### 3. 일반 사용자 인증
```bash
curl --user user:user123 http://localhost:8080/v6/jdbc/secured
```
**예상 결과**: 200 OK

### 학습 포인트
- JdbcUserDetailsManager의 고정 스키마 (users, authorities) 이해
- 유연성 부족: 비즈니스 요구사항에 맞는 테이블 설계 어려움
- DaoAuthenticationProvider가 자동으로 비밀번호 검증

---

## V7 - Custom UserDetailsService 테스트 (권장 방식)

**목적**: 커스텀 도메인 모델(Customer)로 유연한 인증 구현

### 특징
- Customer 도메인 모델 사용
- CustomerRepository를 통한 DB 조회
- Role-based 권한 부여 (hasRole)

### 테스트 시나리오

#### 1. 공개 엔드포인트
```bash
curl http://localhost:8080/v7/custom/public
```
**예상 결과**: 200 OK

#### 2. 보호된 엔드포인트
```bash
curl --user admin@example.com:admin123 http://localhost:8080/v7/custom/secured
```
**예상 결과**: 200 OK (Customer Repository에서 조회)

#### 3. USER 권한 엔드포인트
```bash
curl --user user@example.com:user123 http://localhost:8080/v7/custom/user
```
**예상 결과**: 200 OK (USER role 보유)

#### 4. ADMIN 권한 엔드포인트 (admin으로)
```bash
curl --user admin@example.com:admin123 http://localhost:8080/v7/custom/admin
```
**예상 결과**: 200 OK (ADMIN role 보유)

#### 5. ADMIN 권한 엔드포인트 (user로 접근 시도)
```bash
curl --user user@example.com:user123 http://localhost:8080/v7/custom/admin
```
**예상 결과**: 403 Forbidden (USER role만 보유, ADMIN 권한 없음)

### 학습 포인트
- CustomUserDetailsService로 비즈니스 도메인에 맞는 인증 구현
- CustomerRepository 인터페이스로 추상화 (실제 DB로 교체 용이)
- Role-based 권한 부여: `hasRole("ADMIN")`
- V6보다 실무에 적합한 방식

---

## V8 - 회원가입 + Password Encoding 테스트

**목적**: PasswordEncoder를 사용한 비밀번호 해싱, 회원가입 플로우 이해

### 특징
- 회원가입 API 제공
- BCrypt 비밀번호 해싱
- 이메일 중복 체크

### 테스트 시나리오

#### 1. 공개 엔드포인트
```bash
curl http://localhost:8080/v8/register/public
```
**예상 결과**: 200 OK

#### 2. 회원가입 (신규 사용자 등록)
```bash
curl --request POST \
  --header "Content-Type: application/json" \
  --data '{"email":"test@test.com","pwd":"test123","name":"Test User"}' \
  http://localhost:8080/v8/register/signup
```
**예상 결과**: 201 Created + 등록 성공 메시지

#### 3. 등록된 사용자 확인 (Debug 엔드포인트)
```bash
curl http://localhost:8080/debug/customers
```
**예상 결과**: 
- test@test.com 사용자 존재
- 비밀번호가 `$2a$10$...` 형태로 저장됨 (BCrypt 해시)

#### 4. 등록한 사용자로 로그인
```bash
curl --user test@test.com:test123 http://localhost:8080/v8/register/myinfo
```
**예상 결과**: 200 OK + 사용자 정보 반환

#### 5. 중복 이메일로 가입 시도
```bash
curl --request POST \
  --header "Content-Type: application/json" \
  --data '{"email":"test@test.com","pwd":"test123","name":"Test User"}' \
  http://localhost:8080/v8/register/signup
```
**예상 결과**: 400 Bad Request (이메일 중복 오류)

#### 6. 잘못된 비밀번호로 로그인 시도
```bash
curl --user test@test.com:wrongpassword http://localhost:8080/v8/register/myinfo
```
**예상 결과**: 401 Unauthorized

### 학습 포인트
- 회원가입: `passwordEncoder.encode()` → BCrypt 해시 생성 (`$2a$10$...`, 60자)
- 로그인: `DaoAuthenticationProvider`가 `matches()` 자동 호출하여 비밀번호 검증
- BCrypt는 매번 다른 해시 생성 (salt 포함)
- 회원가입 엔드포인트는 `permitAll()` (인증 불필요)

---

## V9 - Custom AuthenticationProvider 테스트

**목적**: AuthenticationProvider를 직접 구현하여 비즈니스 로직 포함 인증 처리

### 특징
- CustomAuthenticationProvider 직접 구현
- 비밀번호 검증 + 추가 비즈니스 로직 (도메인 검증)
- UserDetailsService 무시됨 (배타적 관계)

### 추가 비즈니스 로직
- **도메인 검증**: `@example.com` 도메인만 로그인 허용

### 테스트 시나리오

#### 1. 공개 엔드포인트
```bash
curl http://localhost:8080/v9/authprovider/public
```
**예상 결과**: 200 OK

#### 2. @example.com 도메인으로 로그인 (성공)
```bash
curl --user admin@example.com:admin123 http://localhost:8080/v9/authprovider/secured
```
**예상 결과**: 200 OK (도메인 검증 통과)

#### 3. 비즈니스 로직 검증 확인
```bash
curl --user user@example.com:user123 http://localhost:8080/v9/authprovider/business-check
```
**예상 결과**: 200 OK + 도메인 검증 결과 표시

#### 4. 다른 도메인으로 회원가입 후 로그인 시도
```bash
# 1. 다른 도메인 사용자 등록
curl --request POST \
  --header "Content-Type: application/json" \
  --data '{"email":"test@wrongdomain.com","pwd":"test123","name":"Wrong Domain"}' \
  http://localhost:8080/v8/register/signup

# 2. 로그인 시도 (비밀번호는 맞지만 도메인 검증 실패)
curl --user test@wrongdomain.com:test123 http://localhost:8080/v9/authprovider/secured
```
**예상 결과**: 401 Unauthorized (도메인 검증 실패: "Only @example.com domain allowed!")

### 학습 포인트
- CustomAuthenticationProvider가 모든 인증 로직 제어
- DB 조회, 비밀번호 검증, 추가 비즈니스 로직 모두 직접 구현
- `passwordEncoder.matches()` 직접 호출 필요
- AuthenticationProvider Bean 등록 시 DaoAuthenticationProvider 생성 안 됨
- UserDetailsService Bean이 있어도 **무시됨** (배타적 관계)
- 유연성 높지만 코드 복잡도 증가

### UserDetailsService vs AuthenticationProvider 비교

| 방식 | DB 조회 | 비밀번호 검증 | 추가 로직 | 복잡도 |
|------|--------|-------------|----------|--------|
| UserDetailsService (V7) | loadUserByUsername() | 자동 (DaoAuthenticationProvider) | 불가능 | 낮음 |
| AuthenticationProvider (V9) | authenticate() 내에서 직접 | 직접 matches() 호출 | 자유롭게 추가 | 높음 |

---

## V10 - Authentication Failure Handler 테스트

**목적**: 인증 실패 시 커스텀 응답 처리, 사용자 친화적 오류 메시지

### 특징
- CustomAuthenticationFailureHandler 구현
- 예외 타입별로 다른 메시지 제공
- JSON 형태 오류 응답

### 테스트 시나리오

#### 1. 공개 엔드포인트
```bash
curl http://localhost:8080/v10/failure/public
```
**예상 결과**: 200 OK

#### 2. 정상 로그인 (HTTP Basic)
```bash
curl --user admin@example.com:admin123 http://localhost:8080/v10/failure/secured
```
**예상 결과**: 200 OK

#### 3. 브라우저로 로그인 실패 테스트 (Form Login)
브라우저로 접근:
```
http://localhost:8080/v10/failure/secured
```

1. 로그인 페이지로 리다이렉트됨
2. 잘못된 비밀번호 입력 (예: `admin@example.com` / `wrongpassword`)
3. JSON 형태의 오류 응답 확인:

```json
{
  "error": "Unauthorized",
  "errorType": "BadCredentialsException",
  "message": "Invalid username or password",
  "timestamp": "2026-01-27T10:30:00",
  "path": "/login"
}
```

#### 4. curl로 로그인 실패 (HTTP Basic - failure handler 미적용)
```bash
curl --user admin@example.com:wrongpassword http://localhost:8080/v10/failure/secured
```
**예상 결과**: 401 Unauthorized (HTTP Basic은 failure handler 적용 안 됨)

### 학습 포인트
- CustomAuthenticationFailureHandler로 사용자 친화적 오류 응답
- 예외 타입별 메시지 구분 가능 (BadCredentialsException, UsernameNotFoundException 등)
- 보안 주의: 실제 운영에서는 "사용자 없음" vs "비밀번호 틀림" 구분하지 않음
- Form Login에만 적용됨 (HTTP Basic은 미적용)

---

## Debug 엔드포인트 테스트 (WEEK 2 추가)

**목적**: 런타임에 활성화된 필터 체인 확인

### 테스트 시나리오

#### 1. 모든 필터 목록 조회
```bash
curl http://localhost:8080/debug/filters
```
**예상 결과**: 각 SecurityFilterChain의 상세 필터 목록

**확인 사항**:
- V1: ~10개 필터 (인증 필터 없음)
- V2: ~13개 필터 (UsernamePasswordAuthenticationFilter 추가)
- V3: ~11개 필터 (BasicAuthenticationFilter 추가)
- V4: ~10개 필터 (CsrfFilter 제거)
- V5 API: ~10개 필터 (BasicAuthenticationFilter, STATELESS)
- V5 Admin: ~13개 필터 (UsernamePasswordAuthenticationFilter)

#### 2. SecurityFilterChain 요약
```bash
curl http://localhost:8080/debug/chains
```
**예상 결과**: 각 체인의 URL 패턴과 필터 개수

#### 3. 현재 인증 사용자 (미인증 상태)
```bash
curl http://localhost:8080/debug/current-user
```
**예상 결과**: 
```json
{
  "authenticated": true,
  "name": "anonymousUser",
  "authenticationType": "AnonymousAuthenticationToken"
}
```

#### 4. 현재 인증 사용자 (인증 후)
```bash
curl -u admin:12345 http://localhost:8080/v3/httpbasic/secured
curl http://localhost:8080/debug/current-user
```
**예상 결과**: admin 사용자 정보 (세션에 따라 다름)

#### 5. 전체 요약
```bash
curl http://localhost:8080/debug/summary
```
**예상 결과**: 
- 전체 SecurityFilterChain 개수
- 각 체인의 필터 개수
- 현재 인증 정보
- 테스트 가이드
- 필터 비교

#### 6. 필터 순서 가이드
```bash
curl http://localhost:8080/debug/filter-order-guide
```
**예상 결과**: Spring Security 표준 필터 순서 및 설정별 필터 매핑

#### 7. Authentication Providers 조회 (WEEK 2)
```bash
curl http://localhost:8080/debug/authentication-providers
```
**예상 결과**: 
- 등록된 AuthenticationProvider 목록
- V6, V7, V8, V10: DaoAuthenticationProvider
- V9: CustomAuthenticationProvider

**학습 포인트**:
- UserDetailsService 방식 → DaoAuthenticationProvider 자동 생성
- AuthenticationProvider 방식 → 커스텀 Provider 사용, DaoAuthenticationProvider 생성 안 됨

#### 8. Customer 데이터 조회 (WEEK 2)
```bash
curl http://localhost:8080/debug/customers
```
**예상 결과**:
- 인메모리 Repository에 저장된 모든 Customer 목록
- 비밀번호 해시 확인 (`$2a$10$...`)
- 회원가입으로 등록한 사용자 확인

**보안 주의**: 실제 운영 환경에서는 이런 엔드포인트를 제공하면 안 됨

---

## 로그 확인

애플리케이션 실행 시 콘솔에서 다음 로그 확인:

### 1. 각 요청마다 필터 체인 실행 로그

```
DEBUG o.s.security.web.FilterChainProxy - Securing GET /v1/basic/public
DEBUG o.s.security.web.FilterChainProxy - /v1/basic/public at position 1 of 10 in additional filter chain; firing Filter: 'WebAsyncManagerIntegrationFilter'
DEBUG o.s.security.web.FilterChainProxy - /v1/basic/public at position 2 of 10 in additional filter chain; firing Filter: 'SecurityContextHolderFilter'
...
```

### 2. 인증 성공 로그 (폼 로그인)

```
DEBUG o.s.s.w.a.UsernamePasswordAuthenticationFilter - Set SecurityContextHolder to UsernamePasswordAuthenticationToken [Principal=admin, Credentials=[PROTECTED], Authenticated=true, Details=WebAuthenticationDetails, Granted Authorities=[ROLE_ADMIN, ROLE_USER]]
```

### 3. 인증 성공 로그 (HTTP Basic)

```
DEBUG o.s.s.w.a.www.BasicAuthenticationFilter - Set SecurityContextHolder to UsernamePasswordAuthenticationToken [Principal=admin, Credentials=[PROTECTED], Authenticated=true]
```

### 4. SecurityContext 저장 로그

```
DEBUG o.s.s.w.c.HttpSessionSecurityContextRepository - Stored SecurityContext for user admin
```

---

## 필터 체인 비교 요약

### WEEK 1 필터 체인

| 버전 | URL 패턴 | 인증 방식 | CSRF | 세션 정책 | 주요 추가/제거 필터 |
|------|----------|----------|------|----------|-------------------|
| V1 | /v1/basic/** | 없음 | 활성화 | Stateful | 기본 필터만 (인증 필터 없음) |
| V2 | /v2/formlogin/** | 폼 로그인 | 활성화 | Stateful | +UsernamePasswordAuthenticationFilter, +DefaultLoginPageGeneratingFilter |
| V3 | /v3/httpbasic/** | HTTP Basic | 활성화 | Stateful | +BasicAuthenticationFilter |
| V4 | /v4/nocsrf/** | HTTP Basic | 비활성화 | STATELESS | -CsrfFilter, SessionManagementFilter(STATELESS 모드) |
| V5 API | /v5/api/** | HTTP Basic | 비활성화 | STATELESS | -CsrfFilter, +BasicAuthenticationFilter |
| V5 Admin | /v5/admin/** | 폼 로그인 | 활성화 | Stateful | +UsernamePasswordAuthenticationFilter, AuthorizationFilter(hasRole) |

### WEEK 2 인증 Provider 비교

| 버전 | URL 패턴 | 인증 방식 | UserDetailsService | AuthenticationProvider | 특징 |
|------|----------|----------|-------------------|----------------------|------|
| V6 | /v6/jdbc/** | HTTP Basic | JdbcStyleUserDetailsService | DaoAuthenticationProvider | JDBC 표준 스키마 시뮬레이션 |
| V7 | /v7/custom/** | HTTP Basic | CustomUserDetailsService | DaoAuthenticationProvider | 커스텀 도메인 모델 (권장) |
| V8 | /v8/register/** | HTTP Basic | CustomUserDetailsService | DaoAuthenticationProvider | 회원가입 + PasswordEncoder |
| V9 | /v9/authprovider/** | HTTP Basic | 무시됨 | CustomAuthenticationProvider | 비즈니스 로직 포함 인증 |
| V10 | /v10/failure/** | Form Login | CustomUserDetailsService | DaoAuthenticationProvider | 커스텀 Failure Handler |

---

## 트러블슈팅

### 1. 403 Forbidden 대신 401 Unauthorized가 나오는 경우
- 인증 필터(formLogin, httpBasic)가 설정되어 있는지 확인
- ExceptionTranslationFilter가 인증 방법에 따라 다르게 처리

### 2. CSRF 토큰 오류
- V2, V3, V5 Admin은 CSRF 활성화되어 있음
- POST 요청 시 CSRF 토큰 필요 (브라우저 사용 권장)
- REST 클라이언트에서는 V4 또는 V5 API 사용

### 3. 세션이 유지되지 않는 경우
- V4, V5 API는 STATELESS 모드이므로 매 요청마다 인증 필요
- V2, V3, V5 Admin은 세션 기반이므로 쿠키 확인

### 4. user 계정으로 /v5/admin 접근 시 403
- 정상 동작: user는 ROLE_USER만 가지고 ROLE_ADMIN 없음
- admin 계정으로 로그인 필요

### 5. V9에서 다른 도메인 로그인 실패
- 정상 동작: CustomAuthenticationProvider가 @example.com 도메인만 허용
- 비밀번호가 맞아도 도메인 검증 실패 시 401

### 6. WEEK 2에서 이메일/비밀번호 다름
- WEEK 1: username=admin, password=12345
- WEEK 2: email=admin@example.com, password=admin123

---

## 다음 단계

1. 각 버전의 엔드포인트를 테스트하며 로그 확인
2. `/debug/filters`로 필터 체인 차이 확인
3. 브라우저 개발자 도구에서 네트워크 탭 확인 (헤더, 쿠키)
4. Postman Collection 생성하여 자동화 테스트
5. WEEK 2 학습으로 넘어가기 전 복습

---

## 학습 체크리스트

### WEEK 1: 필터 체인 기본 동작

- [ ] V1: 인증 방법이 없을 때 403 Forbidden 확인
- [ ] V2: 폼 로그인 페이지 자동 생성 확인
- [ ] V2: 로그인 후 세션 유지 확인
- [ ] V3: curl로 HTTP Basic 인증 성공 확인
- [ ] V3: Authorization 헤더 형식 이해
- [ ] V4: POST 요청 시 CSRF 토큰 불필요 확인
- [ ] V4: STATELESS 모드로 세션 생성 안 됨 확인
- [ ] V5: API와 Admin의 필터 체인 차이 확인
- [ ] V5: user 계정으로 /v5/admin 접근 시 403 확인
- [ ] Debug: 각 체인의 필터 목록 확인
- [ ] 로그: FilterChainProxy 실행 과정 확인
- [ ] 로그: 인증 성공/실패 과정 확인

### WEEK 2: DB 연동 인증과 비밀번호 암호화

- [ ] V6: JDBC 스타일 UserDetailsService로 인증 확인
- [ ] V7: Custom UserDetailsService로 인증 확인
- [ ] V7: Role-based 권한 부여 (user가 /admin 접근 시 403) 확인
- [ ] V8: 회원가입 API로 신규 사용자 등록
- [ ] V8: 등록한 사용자로 로그인 성공 확인
- [ ] V8: /debug/customers에서 BCrypt 해시 확인 (`$2a$10$...`)
- [ ] V9: CustomAuthenticationProvider로 도메인 검증 확인
- [ ] V9: 다른 도메인으로 로그인 시도 시 401 확인
- [ ] V10: 브라우저에서 잘못된 비밀번호 입력 시 JSON 오류 응답 확인
- [ ] Debug: /debug/authentication-providers로 Provider 목록 확인
- [ ] Debug: /debug/customers로 Customer 데이터 확인
- [ ] 학습: UserDetailsService vs AuthenticationProvider 차이 이해

모든 체크리스트 완료 시 WEEK 1, 2 학습 완료! 🎉

