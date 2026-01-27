# 수정 사항 (2026-01-27)

## 문제점 및 해결

### 1. ❌ 문제: /login 페이지 404 에러

**원인**:
- V2 FormLoginSecurityConfig: `.securityMatcher("/v2/formlogin/**")`
- V5 AdminSecurityConfig: `.securityMatcher("/v5/admin/**")`
- `/login`, `/logout` 경로가 어느 SecurityFilterChain에도 매칭되지 않음
- formLogin()의 기본 로그인 페이지가 활성화되지 않음

**해결**:
```java
// V2 FormLoginSecurityConfig
.securityMatcher("/v2/formlogin/**", "/login", "/logout")

// V5 MultiChainSecurityConfig - adminSecurityFilterChain
.securityMatcher("/v5/admin/**", "/login", "/logout")
```

**테스트 결과**:
```bash
# V2 접근 시 /login으로 리디렉션
curl -i http://localhost:8080/v2/formlogin/secured
→ 302 Redirect to http://localhost:8080/login

# /login 페이지 정상 표시
curl -i http://localhost:8080/login
→ 200 OK, HTML 로그인 폼 반환

# V5 Admin 접근 시 /login으로 리디렉션
curl -i http://localhost:8080/v5/admin/dashboard
→ 302 Redirect to http://localhost:8080/login
```

---

### 2. ✅ curl 명령어 full name 적용

**변경사항**: TEST_GUIDE.md의 모든 curl 명령어를 축약형에서 full name으로 변경

**변경 예시**:
```bash
# Before
curl -u admin:12345 http://localhost:8080/v3/httpbasic/secured
curl -X POST -u admin:12345 -H "Content-Type: application/json" -d '{"data":"test"}' ...

# After
curl --user admin:12345 http://localhost:8080/v3/httpbasic/secured
curl --request POST --user admin:12345 --header "Content-Type: application/json" --data '{"data":"test"}' ...
```

**옵션 매핑**:
- `-u` → `--user`
- `-X POST` → `--request POST`
- `-H` → `--header`
- `-d` → `--data`
- `-i` → `--include`

**장점**:
- 가독성 향상
- 명령어 의미 명확화
- 학습자 이해도 향상

---

### 3. ✅ V1 기본 동작 확인

**상황**:
- V1 `/v1/basic/secured` 접근 시 403 Forbidden 반환
- 로그: `Http403ForbiddenEntryPoint: Pre-authenticated entry point called. Rejecting access`

**분석**:
- ✅ **정상 동작**
- V1은 formLogin(), httpBasic() 설정이 없어 인증 방법 없음
- AnonymousAuthenticationFilter가 익명 권한 부여
- 하지만 `.authenticated()` 요구로 접근 거부
- 401 Unauthorized가 아닌 403 Forbidden인 이유:
  - 익명 사용자도 "인증된" 상태로 간주되나, authenticated()는 실제 인증된 사용자만 허용
  - Pre-authenticated entry point가 이를 감지하여 403 반환

**결론**: 문서 설명과 일치하며 수정 불필요

---

## 수정된 파일 목록

1. **FormLoginSecurityConfig.java**
   - securityMatcher에 `/login`, `/logout` 추가

2. **MultiChainSecurityConfig.java**
   - adminSecurityFilterChain의 securityMatcher에 `/login`, `/logout` 추가

3. **TEST_GUIDE.md**
   - 모든 curl 명령어를 full name으로 변경
   - 옵션 설명 추가

4. **README.md**
   - 기본 테스트 섹션의 curl 명령어 업데이트

---

## 검증 완료

✅ V1: /v1/basic/secured → 403 Forbidden (정상)
✅ V2: /v2/formlogin/secured → 302 Redirect to /login → 로그인 페이지 표시
✅ V3: curl --user admin:12345 /v3/httpbasic/secured → 200 OK
✅ V4: curl --request POST --user admin:12345 ... /v4/nocsrf/create → 200 OK
✅ V5: /v5/admin/dashboard → 302 Redirect to /login → 로그인 페이지 표시
✅ V5: curl --user admin:12345 /v5/api/data → 200 OK

---

## 다중 SecurityFilterChain 사용 시 주의사항

**교훈**: 다중 SecurityFilterChain을 사용할 때 공통 경로(/login, /logout 등)를 명시적으로 매칭해야 합니다.

**패턴**:
```java
// 잘못된 예
.securityMatcher("/myapp/**")
.formLogin(withDefaults())  // /login이 매칭되지 않아 404

// 올바른 예
.securityMatcher("/myapp/**", "/login", "/logout")
.formLogin(withDefaults())  // /login이 정상 동작
```

**대안**:
1. 각 체인에 `/login`, `/logout` 명시 (채택)
2. 기본 SecurityFilterChain 추가 (Order를 가장 낮게)
3. 커스텀 로그인 페이지 경로 지정 (`.formLogin(form -> form.loginPage("/myapp/login"))`)

---

**수정 완료일**: 2026-01-27
**테스트 완료**: ✅ 모든 시나리오 검증 완료

