FROM openjdk:17-jdk-alpine

# gradle 설정으로 plain jar 파일은 생성되지 않습니다.
WORKDIR /app

RUN mkdir -p /app/logs && chmod -R 777 /app/logs

COPY app/app.jar app.jar

EXPOSE 8080
EXPOSE 9090

# 4. 컨테이너에서 실행할 명령어 설정
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
