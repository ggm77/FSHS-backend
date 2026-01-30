# FSHS (File Streaming Home Server) - Backend v2

> **복잡한 구축 과정 없이, 개인 PC 환경 그대로의 파일을 실시간 스트리밍 서비스로 구축해 주는 스탠드얼론 NAS 앱입니다.**

---

## 1. 프로젝트 개요

### 1.1 배경 및 목표
* **OS 독립성**: 특정 NAS OS가 아닌 일반 OS(Windows, macOS, Linux) 위에서 프로그램 형태로 동작하도록 설계되었습니다.
* **실시간 스트리밍**: 브라우저에서 지원하지 않는 코덱의 경우 FFmpeg를 통해 실시간 트랜스코딩 스트리밍을 제공합니다.
* **간편한 마이그레이션**: 기존 파일 시스템을 그대로 인식하므로, 별도의 데이터 이동 없이 설치 즉시 NAS로 활용 가능합니다.
* **현대적 UI/UX**: 깔끔하고 직관적인 인터페이스를 제공합니다.

### 1.2 핵심 기능
* **파일 관리**: 업로드, 다운로드, 파일 탐색 및 검색 기능 제공.
* **파일 스트리밍**: 이미지/비디오 갤러리 뷰 및 음악/영상 재생목록 기능.
* **다중 사용자 지원**: 사용자별 루트 폴더 분리를 통한 독립적 저장 공간 제공.
* **파일 동기화**: 실제 디스크의 파일 정보와 DB 캐시 정보를 일치시키는 동기화 기능.
* **파일 공유**: 파일 공유 링크 생성 기능 제공.

---

## 2. 시스템 아키텍처

### 2.1 기술 스택
* **Language**: Java 25
* **Framework**: Spring Boot 4.0.2
* **Database**: SQLite 3.51.0
* **Media Processing**: FFmpeg

### 2.2 API 명세서
[![API Docs](https://img.shields.io/badge/API_명세서-333333?style=for-the-badge&logo=read-the-docs&logoColor=white)](https://agreeable-canary-131.notion.site/v2-API-2ef214c948c380cf9853ebef1ed947c9)

### 2.3 프로젝트 구조
```text
com.seohamin.fshs.v2
 ├ domain
 |  ├ auth (인증 관련 API)
 |  ├ file (파일 관련 API)
 |  ├ folder (폴더 관련 API)
 |  ├ user (유저 관련 API)
 |  ├ share (공유 관련 API)
 |  └ system (시스템 관련 API)
 └ global
    ├ auth (각종 인증 관련 설정)
    ├ config (스웨거 등 공통 설정)
    ├ util (유틸 도구 모음)
    └ exception (커스텀 예외 처리)
```

---

## 3. 데이터베이스 설계 (ERD)

시스템은 SQLite를 사용하여 파일 메타데이터를 캐싱하며, 실제 파일 시스템과 유기적으로 연동됩니다.

![FSHS ERD](https://github.com/user-attachments/assets/a80171ca-6d56-4b6e-b3b6-a2530b4e2fa7)

* **User**: 사용자 계정 정보 및 권한(Role)을 관리합니다.
* **Folder**: 유저별로 독립된 루트 폴더를 가지며, 디렉토리 계층 구조를 저장합니다.
* **File**: 파일의 상대 경로, 크기, MIME 타입, 미디어 코덱 정보 및 카테고리(IMAGE, VIDEO 등)를 저장하여 빠른 조회를 지원합니다.

---

## 4. 커밋 컨벤션

본 프로젝트는 일관된 히스토리 관리를 위해 아래의 커밋 메세지 형식을 준수합니다.

| Type | Description | Example                   |
| :--- | :--- |:--------------------------|
| **feat** | 새로운 기능 추가, 구현 | feat: login 기능 구현         |
| **fix** | 버그 픽스 | fix: color 버그 수정          |
| **refactor** | 코드 리팩토링 | refactor: 변수명 수정          |
| **chore** | 파일 이동, 경로 변경, 이름 변경 | chore: feet -> feat 이름 변경 |
| **docs** | 주석이나 리드미 수정 | docs: 리드미 수정              |
| **style** | UI 작업, 스타일 관련 파일 추가 및 수정 | style: 폰트 등록              |
| **test** | 테스트 케이스 생성 | test: 테스트 케이스 생성          |

---

## 5. 개발 로드맵

### 5.1 v2 로드맵
1.  [ ] 세션 기반 인증 방식 구현 및 유저 API
2.  [ ] 파일 및 폴더 조회/관리 API 구현
3.  [ ] FFmpeg 기반 실시간 트랜스코딩 스트리밍 구현
4.  [ ] 디스크 정보 동기화 및 파일 탐색 기능
5.  [ ] 자동 파일 변경 감지 체크 기능
6.  [ ] 파일 공유 링크 생성 및 미디어 재생목록 기능

### 5.2 향후 확장 계획
* 하나의 루트 폴더를 여러 유저가 공동으로 관리하는 기능 지원
* UPS 연동을 통한 안전한 서버 종료 지원
* 부분 업로드/다운로드를 통한 대용량 파일(100GB+) 지원