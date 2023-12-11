# FSHS

---

>## File Streaming Home Server
>  클라우드 서비스와 음악 스트리밍, 영상 스트리밍 서비스를 제공하는 프로젝트이다.
>  음악과 영상은 클라우드에 올라온 정보들을 기반으로 한다.


---

## 개발 환경
+ Java - OpenJDK 17.0.8.1
+ IDE : IntelliJ 2023.2
+ Framework : SpringBoot 3.2.0
+ ORM : JPA
+ Database Version : MariaDB 10.5.21

---

## 주요기능
>### <a href="https://github.com/ggm77/FSHS-backend/blob/main/src/main/java/org/iptime/raspinas/FSHS/controller/userFile/UserFileController.java">파일 업로드, 다운로드 삭제</a>
> 유저가 원하는 파일을 원하는 개수 만큼 업로드 할 수 있다. 또한 유저가 원하는 파일만 다운로드 받거나 삭제 할 수 있다.
> 
> 오디오 파일과 영상 파일은 자동으로 HLS 프로토콜로 제공 될 수 있도록 변환된다.

>### <a href="https://github.com/ggm77/FSHS-backend/blob/main/src/main/java/org/iptime/raspinas/FSHS/controller/userFileStreaming/UserFileStreamingController.java">이미지, 오디오, 영상 스트리밍</a>
> 유저가 업로드한 이미지를 조회 할 수 있고, 오디오나 영상 파일을 HLS 프로토콜을 이용해서 스트리밍 할 수 있다. 

>### <a href="https://github.com/ggm77/FSHS-backend/blob/main/src/main/java/org/iptime/raspinas/FSHS/controller/auth/signIn/SignInController.java">로그인</a> 및 <a href="https://github.com/ggm77/FSHS-backend/blob/main/src/main/java/org/iptime/raspinas/FSHS/controller/auth/signUp/SignUpController.java">회원가입</a>
> Jwt 토큰을 이용한 회원가입과 로그인을 제공해서 유저를 구분한다.

---

## API
+ POST - [ /api/v1/file ]  파일을 업로드한다.
+ GET - [ /api/v1/file/{id} ]  원하는 파일을 다운로드한다.
+ DELETE - [ /api/v1/file/{id} ]  원하는 파일을 삭제한다.


+ GET - [ /api/v1/streaming-image/{userId}/{path}/{fileNameAndExtension} ]  원하는 이미지 파일을 조회한다.
+ GET - [ /api/v1/streaming-audio/{userId}/{path}/{fileName}/{hlsFile} ]  원하는 오디오 파일을 HLS 프로토콜을 통해 스트리밍 한다.
+ GET - [ /api/v1/streaming-video/{userId}/{path}/{fileName}/{hlsFile} ]  원하는 영상 파일을 HLS 프로토콜을 통해 스트리밍 한다.


+ POST - [ /api/v1/auth/sign-in ]  로그인을 제공한다.
+ POST - [ /api/v1/auth/sign-up ]  회원가입을 제공한다.


+ GET - [ /api/v1/user/{id} ]  자신의 정보를 조회할 수 있다.
+ PATCH - [ /api/v1/user/{id} ]  자신의 정보를 수정 할 수 있다.
+ DELETE - [ /api/v1/user/{id} ] 회원 탈퇴를 할 수 있다.