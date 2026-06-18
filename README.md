# RealPlan Backend

RealPlan은 사용자의 학습/작업 태스크를 등록하고, AI가 예상 소요시간과 일정 배치를 보정해주는 캡스톤 프로젝트입니다. 이 저장소는 RealPlan의 백엔드 API 서버(Spring Boot)를 담고 있습니다. AI 추론 로직은 별도의 Python(FastAPI) 서비스(`realplan-ai`)에서 처리하며, 백엔드는 이 AI 서비스를 HTTP로 호출하는 클라이언트 역할을 겸합니다.

## 1. 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 언어 / 빌드 | Java 21, Gradle |
| 프레임워크 | Spring Boot 4.0.5 (spring-boot-starter-webmvc, data-jpa, security, jdbc, flyway) |
| 데이터베이스 | PostgreSQL 16 (Docker), 스키마 마이그레이션은 Flyway |
| 인증 | JWT (Access/Refresh 이중 토큰, 자체 구현) + Spring Security |
| API 문서 | springdoc-openapi-starter-webmvc-ui 3.0.1 (Swagger UI) |
| 테스트 | JUnit 5, spring-boot-starter-*-test, H2 |
| 외부 연동 | AI 서비스(FastAPI)와 HTTP 통신, 미연결 시 규칙 기반 Fallback으로 대체 |

## 2. 로컬 실행 방법

### 2.1 요구사항

- JDK 21
- Docker / Docker Compose (PostgreSQL 실행용)

### 2.2 데이터베이스 실행

저장소 루트의 `realplan/` 디렉터리에 Gradle 프로젝트와 `docker-compose.yml`이 위치합니다. 아래 명령으로 PostgreSQL 컨테이너를 먼저 띄웁니다.

```bash
cd realplan
docker compose up -d
```

`docker-compose.yml`은 `realplan-postgres` 컨테이너를 `localhost:5433`에 노출하며, 데이터베이스/사용자/비밀번호는 모두 `realplan`으로 설정되어 있습니다(로컬 개발용 기본값이므로 운영 환경에서는 반드시 교체해야 합니다).

### 2.3 환경 설정

`src/main/resources/application.properties`에 정의된 주요 설정값은 다음과 같습니다.

| 키 | 기본값 | 설명 |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/realplan` | docker-compose의 포트(5433)와 일치해야 함 |
| `spring.jpa.hibernate.ddl-auto` | `validate` | 스키마는 Flyway가 관리하며, JPA는 엔티티-스키마 일치 여부만 검증 |
| `spring.flyway.locations` | `classpath:db/migration,classpath:db/seed` | 마이그레이션과 시드 데이터 경로 분리 |
| `jwt.secret` | (로컬 개발용 문자열) | HMAC-SHA256 서명 키. 운영 환경에서는 환경변수로 주입 필요 |
| `jwt.access-token-expiry` | `1800000` (30분) | Access Token 유효시간(ms) |
| `jwt.refresh-token-expiry` | `604800000` (7일) | Refresh Token 유효시간(ms) |
| `ai.enabled` | `true` | `false`로 설정하면 AI 서비스 호출 없이 `FallbackAiClient`(규칙 기반)가 대신 동작 |
| `ai.base-url` | `http://localhost:8000` | AI(FastAPI) 서비스 주소 |
| `ai.timeout-ms` | `5000` | AI 서비스 호출 타임아웃 |

운영/개인 환경에서는 위 값들을 `application.properties`를 직접 수정하거나 `SPRING_DATASOURCE_URL`, `JWT_SECRET`처럼 Spring Boot의 환경변수 오버라이드 규칙(`-D` 옵션 또는 OS 환경변수)으로 주입하는 것을 권장합니다. 비밀번호나 시크릿 값을 그대로 커밋하지 않도록 주의해야 합니다.

### 2.4 빌드 및 실행

```bash
cd realplan
./gradlew bootRun
```

서버가 정상적으로 기동되면 `GET /api/health`로 헬스체크를 확인할 수 있고, `http://localhost:8080/swagger-ui/index.html`에서 전체 API 명세를 확인할 수 있습니다. 인증이 필요한 API는 Swagger UI의 Authorize 버튼에 `Bearer {accessToken}` 형식으로 토큰을 입력하면 테스트할 수 있습니다.

### 2.5 테스트 실행

```bash
./gradlew test
```

테스트는 `src/test/resources/application.properties`에 정의된 별도 설정(H2 등)을 사용하므로 운영 DB에 영향을 주지 않습니다.

## 3. 프로젝트 구조

패키지는 기능 단위(도메인)로 분리되어 있으며, 각 도메인 하위에 `controller / service / repository / entity / dto`를 두는 구조를 따릅니다. 인증, 예외 처리, 공통 응답 형식과 같이 도메인에 걸쳐 공통으로 쓰이는 코드는 `global` 패키지에 모아두었습니다.

```
src/main/java/capstone2/team3/realplan/
├── controller/            # HealthController (헬스체크)
├── domain/
│   ├── auth/               # 회원가입, 로그인, 토큰 재발급, 로그아웃
│   ├── user/                # 사용자 프로필 조회/수정, 회원 탈퇴
│   ├── folder/              # 태스크 분류용 폴더 CRUD
│   ├── task/                 # 태스크 CRUD, 완료 처리, AI 유형 추천
│   ├── tasktype/            # 태스크 유형(시간형/분량형/만족형) 엔티티
│   ├── dailyplan/          # 데일리 플랜 생성·확정, 슬롯 배정, AI 자동 배치
│   ├── session/             # 집중 세션 시작/일시정지/재개/종료/이탈
│   ├── analytics/          # 주간·일별·시간대별·유형별 학습 통계
│   └── ai/                    # AiClient 인터페이스, Http/Fallback 구현체, 보정 계수 서비스
└── global/
    ├── common/             # ApiResponse 공통 응답 래퍼, BaseEntity
    ├── config/               # SecurityConfig, JpaConfig, SwaggerConfig
    ├── exception/           # BusinessException, ErrorCode, GlobalExceptionHandler
    └── security/             # JwtUtil, JwtAuthenticationFilter, TokenStore
```

데이터베이스 스키마는 `src/main/resources/db/migration`의 Flyway 스크립트(V1~V9)로 관리되며, `db/seed`의 반복 적용 가능한(repeatable) 스크립트로 개발용 초기 데이터를 시드합니다.

| 마이그레이션 | 내용 |
|---|---|
| V1 | 초기 스키마 생성 |
| V2 | 태스크 유형 초기 데이터 |
| V3 | 기존 사용자 기본 폴더 백필 |
| V4 | 인증 토큰(Refresh Token, 블랙리스트) 테이블 생성 |
| V5 | 데일리 플랜 세션 분할 테이블 추가 |
| V6 | 슬롯-태스크 배정 구조 추가 |
| V7 | AI 보정 계수/로그 저장 테이블 추가 |
| V8 | 데일리 플랜 세션 제약조건 강화 |
| V9 | 태스크 soft delete 컬럼 추가 |

## 4. API 개요

전체 명세는 Swagger UI(`/swagger-ui/index.html`)에서 확인할 수 있으며, 아래는 도메인별 주요 엔드포인트입니다.

| 도메인 | 메서드/경로 | 설명 |
|---|---|---|
| 인증 | `POST /api/auth/register` | 회원가입 (기본 폴더 자동 생성) |
| 인증 | `POST /api/auth/login` | 로그인, 토큰 발급 |
| 인증 | `POST /api/auth/refresh` | Refresh Token Rotation으로 토큰 재발급 |
| 인증 | `DELETE /api/auth/logout` | Access Token 블랙리스트 등록, Refresh Token 삭제 |
| 사용자 | `GET /api/users/me` | 내 프로필 조회 |
| 사용자 | `PATCH /api/users/me` | 프로필 수정 |
| 사용자 | `DELETE /api/users/me`, `/me/withdraw` | 회원 탈퇴 |
| 폴더 | `GET/POST /api/folders`, `PATCH/DELETE /api/folders/{folderId}` | 폴더 CRUD |
| 태스크 | `GET/POST /api/tasks`, `GET/PATCH/DELETE /api/tasks/{taskId}` | 태스크 CRUD |
| 태스크 | `POST /api/tasks/classify` | AI 기반 태스크 유형 추천(저장 없음) |
| 태스크 | `POST /api/tasks/{taskId}/complete` | 완료 처리 + AI 보정 계수 갱신 |
| 태스크 | `GET /api/tasks/reminders`, `POST /api/tasks/reminders/read` | 마감 리마인더 조회/읽음 처리 |
| 일정 | `GET/POST /api/daily-plans` | 데일리 플랜 조회/생성 |
| 일정 | `GET /api/daily-plans/{planId}/recommend` | AI 태스크 추천 조회 |
| 일정 | `POST /api/daily-plans/{planId}/tasks`, `/tasks/auto` | 수동/AI 자동 슬롯 배정 |
| 일정 | `PATCH/PUT /api/daily-plans/{planId}/...` | 슬롯·태스크·상태 수정 |
| 세션 | `POST /api/sessions` | 집중 세션 시작 |
| 세션 | `PATCH /api/sessions/{sessionId}/pause`, `/resume`, `/abandon` | 일시정지/재개/이탈 |
| 세션 | `POST /api/sessions/{sessionId}/end` | 세션 종료(피드백 저장 + AI 재예측) |
| 세션 | `POST /api/sessions/manual` | 수동 세션 기록 |
| 세션 | `GET /api/tasks/{taskId}/sessions` | 태스크별 세션 목록 |
| 통계 | `GET /api/analytics/weekly`, `/daily`, `/focus-by-hour`, `/type-stats`, `/difficulty-correction` | 통계 조회 |
| 헬스체크 | `GET /api/health` | 서버 상태 확인 |

모든 응답은 `success`, `data`, `error` 세 필드를 갖는 `ApiResponse<T>` 공통 래퍼로 반환됩니다. 성공 시 `data`에 결과가 담기고, 실패 시 `error`에 표준화된 오류 코드와 한국어 메시지가 담깁니다.

## 5. 인증 구조

- Access Token(30분)과 Refresh Token(7일)의 이중 토큰 구조이며, 서명은 HMAC-SHA256을 자체 구현(`JwtUtil`)으로 처리합니다.
- Refresh Token은 원문이 아닌 SHA-256 해시값으로 데이터베이스에 저장되며, 재발급 시 기존 토큰을 폐기하고 새 토큰을 발급하는 Rotation 방식을 사용합니다.
- 로그아웃은 토큰 삭제 대신 Access Token 해시를 블랙리스트 테이블에 등록하는 방식으로, 남은 유효기간 동안에도 토큰을 즉시 무효화합니다.
- `JwtAuthenticationFilter`가 매 요청마다 토큰 서명/만료/블랙리스트 여부를 검증하고, `SecurityConfig`가 인증 실패(401)와 인가 실패(403)를 `ApiResponse` 형식에 맞춰 응답하도록 커스터마이징되어 있습니다.

## 6. AI 서비스 연동

AI 관련 호출은 `AiClient` 인터페이스로 추상화되어 있으며, `ai.enabled` 설정값에 따라 두 구현체 중 하나가 주입됩니다.

- `HttpAiClient` (`ai.enabled=true`): `java.net.http.HttpClient`로 AI(FastAPI) 서비스에 실제 HTTP 요청을 보냅니다. 네트워크 오류, 비정상 상태 코드, 비정상 응답 본문을 모두 `BusinessException(AI_SERVICE_UNAVAILABLE)`로 통일해 던집니다.
- `FallbackAiClient` (`ai.enabled=false`): 외부 호출 없이 규칙 기반 로직으로 동일한 인터페이스를 만족시킵니다. AI 서비스 없이도 백엔드 기능을 독립적으로 실행/테스트할 수 있습니다.

태스크 생성/완료, 세션 종료처럼 AI 호출이 포함된 흐름은 `AI_SERVICE_UNAVAILABLE` 예외만 선택적으로 흡수하여 사용자 입력값으로 대체하는 방식(graceful degradation)으로 구현되어 있어, AI 서비스 장애가 핵심 기능 자체를 막지 않습니다.

## 7. 예외 처리

- `ErrorCode`: HTTP 상태, 코드, 한국어 메시지를 갖는 열거형(현재 24개 코드).
- `BusinessException`: 서비스 계층에서 비즈니스 규칙 위반 시 던지는 공통 예외.
- `GlobalExceptionHandler`(`@RestControllerAdvice`): `BusinessException`, `MethodArgumentNotValidException`(입력값 검증 실패), 그 외 모든 예외를 각각 처리하여 항상 동일한 `ApiResponse` 오류 형식으로 응답합니다.

## 8. 기타 참고

- 태스크 삭제는 hard delete가 아닌 soft delete(`deleted_at` 컬럼)로 처리되어 실수로 인한 데이터 손실을 방지합니다.
- 만료된 Refresh Token/블랙리스트 항목은 별도 로직으로 주기적으로 정리됩니다.
- 본 README는 `origin/dev` 브랜치 기준 코드를 토대로 작성되었습니다. 브랜치/버전이 달라지면 일부 설정값이나 엔드포인트가 변경될 수 있습니다.
