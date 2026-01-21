# FSHS-backend

---

>## File Streaming Home Server - backend
>  클라우드 서비스와 음악 스트리밍, 영상 스트리밍을 제공하는 서비스의 백엔드 입니다.

---

## 개발 환경
+ Java - OpenJDK 17.0.12
+ IDE : IntelliJ 2023.3
+ Framework : SpringBoot 3.2.0
+ ORM : JPA 3.1.5
+ Database Version : MariaDB 10.6.18

---

## 주요기능
>### <a href="https://github.com/ggm77/FSHS-backend/blob/main/src/main/java/org/iptime/raspinas/FSHS/controller/userFile/UserFileController.java">파일 CRUD 및 다운로드</a>
> 유저가 원하는 파일을 업로드 할 수 있습니다. 또한 유저가 원하는 파일만 다운로드 받거나 삭제 할 수 있습니다.
> 
> 오디오 파일과 영상 파일은 자동으로 HLS 프로토콜로 제공 될 수 있도록 변환됩니다.

>### <a href="https://github.com/ggm77/FSHS-backend/blob/main/src/main/java/org/iptime/raspinas/FSHS/controller/userFileStreaming/UserFileStreamingController.java">이미지, 오디오, 영상 스트리밍</a>
> 유저가 업로드한 파일을 스트리밍 받을 수 있습니다.

>### <a href="https://github.com/ggm77/FSHS-backend/blob/main/src/main/java/org/iptime/raspinas/FSHS/controller/auth/signIn/SignInController.java">로그인</a> 및 <a href="https://github.com/ggm77/FSHS-backend/blob/main/src/main/java/org/iptime/raspinas/FSHS/controller/auth/signUp/SignUpController.java">회원가입</a>
> JWT 토큰을 이용한 인증을 제공합니다.

---

