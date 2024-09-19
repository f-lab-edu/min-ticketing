FROM openjdk:17-jdk-alpine

# gradle 설정으로 plain jar 파일은 생성되지 않습니다.
WORKDIR /app

COPY app/app.jar app.jar

# 4. 컨테이너에서 실행할 명령어 설정
ENTRYPOINT ["java", "-jar", "/app/app.jar"]