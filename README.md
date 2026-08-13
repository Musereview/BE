# 🎵 MuseReview Backend

> **연주 데이터를 분석하고 맞춤형 피드백을 제공하는 MuseReview의 Backend Server입니다.**

<br/>

---

## 🛠 Tech Stack

<div align="center">

### Backend

<img src="https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
<img src="https://img.shields.io/badge/JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white"/>

<br/>

### Database & Cache

<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"/>
<img src="https://img.shields.io/badge/Redis-FF4438?style=for-the-badge&logo=redis&logoColor=white"/>

<br/>

### Infra

<img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white"/>
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
<img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white"/>

</div>

<br/>

---


## 🏗 Architecture

<div align="center">

<img width="100%"
alt="MuseReview Backend Architecture" src="https://github.com/user-attachments/assets/941fdffb-b9d7-4d43-8ff9-bf2e0cfaa136" />


</div>

<br/>

**MuseReview**는 안정적인 서비스 운영과 각 구성요소의 역할 분리를 고려하여 위와 같이 **Production 환경**의 인프라를 구성했습니다.

### 저장소에서 관리하는 구성

* `docker-compose.yml`에서 Backend, Redis, Analysis 서비스를 각각 독립된 컨테이너로 정의하고, 하나의 **AWS EC2** 인스턴스에서 함께 운영합니다.
* `.github/workflows/cd.yml`에서 **GitHub Actions**로 이미지를 빌드해 **Docker Hub**에 푸시한 뒤, EC2에서 Docker Compose로 배포하는 과정을 자동화했습니다.
* `application-prod.yml`에서 외부 PostgreSQL, **AWS S3**, OAuth Redirect URI 등 운영 환경 연동 설정을 관리합니다.

### 저장소 외부에서 관리하는 인프라

* **Nginx**: EC2에 직접 설치되어 Reverse Proxy 역할을 담당합니다. (설정 파일은 본 저장소에 포함되지 않습니다.)
* **AWS RDS(PostgreSQL) · AWS S3**: 영구 데이터와 파일 데이터를 분리해 저장하며, AWS 콘솔에서 프로비저닝합니다. 접속 정보는 환경 변수로 주입합니다.
* **OAuth 소셜 로그인 · Gemini API**: 외부 서비스로 연동하여 **운영 편의성, 데이터 안정성, 서비스 간 역할 분리 및 확장성**을 확보했습니다.

<br/>

---

## 👥 Backend Team

<div align="center">

<table>
  <tr>
    <td align="center" width="170">
      <a href="https://github.com/rkdehdrbs7885-oss">
        <img src="https://github.com/rkdehdrbs7885-oss.png" width="100" alt="강동균"/>
      </a>
    </td>
    <td align="center" width="170">
      <a href="https://github.com/on1yoneprivate">
        <img src="https://github.com/on1yoneprivate.png" width="100" alt="고원정"/>
      </a>
    </td>
    <td align="center" width="170">
      <a href="https://github.com/kimyw1018">
        <img src="https://github.com/kimyw1018.png" width="100" alt="김예원"/>
      </a>
    </td>
    <td align="center" width="170">
      <a href="https://github.com/p1001q">
        <img src="https://github.com/p1001q.png" width="100" alt="박수연"/>
      </a>
    </td>
    <td align="center" width="170">
      <a href="https://github.com/ownue">
        <img src="https://github.com/ownue.png" width="100" alt="이은우"/>
      </a>
    </td>
  </tr>

  <tr>
    <td align="center">
      <a href="https://github.com/rkdehdrbs7885-oss"><b>강동균</b></a>
    </td>
    <td align="center">
      <a href="https://github.com/on1yoneprivate"><b>고원정</b></a>
    </td>
    <td align="center">
      <a href="https://github.com/kimyw1018"><b>김예원</b></a>
    </td>
    <td align="center">
      <a href="https://github.com/p1001q"><b>박수연</b></a>
    </td>
    <td align="center">
      <a href="https://github.com/ownue"><b>이은우</b></a>
    </td>
  </tr>

  <tr>
    <td align="center">
      학습 · 백킹트랙<br/>
      알림
    </td>
    <td align="center">
      연주<br/>
      운영 · 인프라
    </td>
    <td align="center">
      인증
    </td>
    <td align="center">
      사용자 · 학습
    </td>
    <td align="center">
      분석 · AI 멘토 · 통계<br/>
      홈 · 히스토리
    </td>
  </tr>
</table>

</div>

<br/>

---

## 📚 Convention

### 🌿 1. Branch Strategy

안정적인 배포와 독립적인 기능 개발을 위해 **Git Flow 기반 브랜치 전략**을 사용합니다.

| Branch                | Description                                              |
| :-------------------- | :------------------------------------------------------- |
| 🔵 **`main`**         | 항상 배포 가능한 상태를 유지하는 운영 브랜치입니다. 직접 작업하지 않고 PR을 통해서만 머지합니다. |
| 🟢 **`develop`**      | 다음 배포를 준비하는 통합 개발 브랜치입니다.                                |
| 🟡 **Feature Branch** | 기능 및 이슈 단위로 `develop`에서 분기하여 작업합니다.                      |

<br/>

#### Branch Naming Convention

```text
Prefix/#이슈번호-작업내용
```

> Kebab Case를 사용합니다.

**Example**

```text
feat/#10-login-api
chore/#1-setting-base
```

|   Prefix   | Description               | Example                     |
| :--------: | :------------------------ | :-------------------------- |
|   `feat`   | 새로운 기능 추가                 | `feat/#10-login-api`        |
|    `fix`   | 버그 수정                     | `fix/#23-header-layout`     |
|   `docs`   | README 등 문서 수정            | `docs/#5-update-readme`     |
|   `style`  | 코드 포맷팅 등 로직에 영향을 주지 않는 변경 | `style/#12-format-code`     |
| `refactor` | 코드 리팩토링                   | `refactor/#30-user-service` |
|   `chore`  | 설정 파일, 빌드 환경 등의 변경        | `chore/#1-setting-base`     |

<br/>

---


### 📝 2. Commit Convention

커밋 메시지는 다음 형식으로 작성합니다.

```text
타입: 작업 내용 (#이슈번호)
```

**Example**

```text
feat: 카카오 소셜 로그인 API 추가 (#10)
```

<br/>

#### 🔓 Pull Request & Code Review

PR은 활발한 코드 리뷰와 안정적인 병합을 위해 **최소 2명 이상의 Approve**를 받은 이후 머지합니다.

<br/>

---


### 🛠️ 3. Development Convention

#### 💻 Environment

| Category  | Version / Technology |
| :-------- | :------------------- |
| Language  | Java 17              |
| Framework | Spring Boot 3.2.2    |
| ORM       | JPA                  |
| Database  | PostgreSQL 18        |

<br/>

#### 🎨 Code Style

프로젝트는 `Spring JavaFormat`을 사용하여 공통 코드 스타일을 유지합니다.

빌드 시 포맷이 맞지 않을 경우 검증에 실패할 수 있으므로 작업 전 코드 스타일을 확인합니다.

| Target            | Convention         | Example          |
| :---------------- | :----------------- | :--------------- |
| Class             | `PascalCase`       | `AuthController` |
| Variable / Method | `camelCase`        | `accessToken`    |
| Constant          | `UPPER_SNAKE_CASE` | `MAX_LIMIT`      |

> ⚠️ 일반 변수 및 메서드명에는 언더바(`_`)를 사용하지 않습니다.

<br/>

---

<br/>

### 🔒 4. Local Environment

DB Password, JWT Secret, OAuth Key 등의 민감 정보는 저장소에 직접 커밋하지 않고 `MR_config`를 통해 관리합니다.

1. 로컬 PostgreSQL에 `mr_db` 데이터베이스를 생성합니다.
2. 프로젝트 루트에 `MR_config/local/` 디렉터리를 생성합니다.
3. 공유된 `application.example.yml`을 해당 디렉터리에 복사합니다.
4. 파일명을 `application.yml`로 변경합니다.
5. 로컬 DB 비밀번호와 필요한 API Key를 입력합니다.
6. 아래 실행 명령으로 애플리케이션을 구동합니다.

<br/>

#### ▶️ Run

Spring Boot Gradle Plugin이 제공하는 `bootRun` 태스크로 애플리케이션을 실행합니다.

**Windows PowerShell**

```shell
./gradlew.bat bootRun
```

**Git Bash / macOS**

```shell
./gradlew bootRun
```

<br/>

#### 🚀 Build

기본 빌드는 컴파일과 테스트를 모두 수행합니다.

```shell
# Windows PowerShell
./gradlew.bat build

# Git Bash / macOS
./gradlew build
```

<br/>

테스트를 건너뛰고 빠르게 패키징만 확인하고 싶을 때는 `-x test`로 `test` 태스크를 제외합니다. 단, PR 전에는 반드시 테스트를 포함한 기본 빌드로 검증합니다.

```shell
# Windows PowerShell
./gradlew.bat build -x test

# Git Bash / macOS
./gradlew build -x test
```

<br/>

#### 📖 Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

<br/>
