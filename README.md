# 🎵 Muse Review - Backend

> **뮤즈리뷰 백엔드 개발 팀 공통 컨벤션 및 가이드북입니다.** > 모든 팀원은 원활한 협업을 위해 아래 규칙을 반드시 준수해 주세요!

---

## 🌿 1. 브랜치 전략 (Git Flow)

우리 팀은 안정적인 배포와 독립적인 기능 개발을 위해 브랜치를 분리하여 관리합니다.

* 🔵 **`main`** : 항상 배포 가능한 상태를 유지하는 운영 브랜치 (직접 작업 금지, PR 머지만 허용)
* 🟢 **`develop`** : 다음 배포를 준비하는 통합 개발 브랜치 (모든 기능 개발본이 모이는 곳)
* 🟡 **`기능 브랜치`** : 기능/이슈 단위로 `develop`에서 파생하여 개발하는 브랜치

### 📌 Branch Naming Convention
* **구조:** `Prefix/#이슈번호-작업내용` (Kebab Case 사용)
* **예시:** `feat/#10-login-api`, `chore/#1-setting-base`

| Prefix | 설명 | 사용 예시 |
| :--- | :--- | :--- |
| `feat` | 새로운 기능 추가 | `feat/#10-login-api` |
| `fix` | 버그 수정 | `fix/#23-header-layout` |
| `docs` | 문서 수정 (README 등) | `docs/#5-update-readme` |
| `style` | 코드 포맷팅 (로직 변경 없음) | `style/#12-format-code` |
| `refactor`| 코드 리팩토링 | `refactor/#30-user-service` |
| `chore` | 설정 파일 변경, 패키지 빌드 등 | `chore/#1-setting-base` |

---

## 📝 2. 커밋 컨벤션 (Commit Convention)

* **메시지 구조:** `타입: 작업 내용 (#이슈번호)` 형식으로 작성 (제목은 50자 이내 소문자 태그 권장)
* **예시:** `feat: 카카오 소셜 로그인 API 추가 (#10)`

### 🔓 PR 머지 및 코드 리뷰 규칙
* **PR 머지 조건:** 활발한 코드 리뷰를 위해 **최소 2명 이상의 승인(Approve)**이 있어야 머지 가능
* **리뷰 태그 가이드:**
    * `P1`: 꼭 반영해 주세요 (중대한 오류 가능성, Request Changes 필수)
    * `P2`: 적극적으로 고려해 주세요 (토큰/논의 권장)
    * `P3`: 웬만하면 반영해 주세요 / 가벼운 의견 (Comment 처리)

---

## 🛠️ 3. 개발 환경 및 코드 컨벤션

### 💻 Tech Stack
* **Language & Framework:** Java 17 / Spring Boot 3.2.2
* **Database:** PostgreSQL 18 / JPA

### 🎨 Code Style (Spring JavaFormat)
우리 프로젝트는 `Spring JavaFormat` 플러그인을 사용하여 공통 포맷을 강제합니다. 빌드 시 포맷이 맞지 않으면 컴파일이 실패할 수 있습니다.
* **클래스명:** `PascalCase` (`class AuthController`)
* **변수/메서드명:** `camelCase` (`String accessToken`)
* **상수명:** `UPPER_SNAKE_CASE` (`int MAX_LIMIT = 3`)
* ⚠️ **주의:** 변수 및 메서드명에 언더바(`_`) 사용을 금지합니다.

---

## 🔒 4. 로컬 환경 세팅 (민감 정보 보호)

민감 정보(DB 패스워드, JWT Secret, OAuth Key) 유출 방지를 위해 `MR_config` 안전 금고 시스템을 사용합니다.

1. 로컬 PostgreSQL에 `mr_db` 데이터베이스를 생성합니다.
2. 프로젝트 루트에 `MR_config/local/` 폴더를 생성합니다.
3. 공유된 `application.example.yml` 파일을 해당 폴더에 복사한 뒤, 파일명을 **`application.yml`**로 변경합니다.
4. 본인의 로컬 DB 비밀번호와 개인 API Key를 입력한 후 서버를 구동합니다.

### 🚀 로컬 빌드 명령어
* **Windows PowerShell:** `./gradlew.bat build -x test`
* **Git Bash / Mac:** `./gradlew build -x test`
* **Swagger UI 주소:** `http://localhost:8080/swagger-ui/index.html`