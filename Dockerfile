FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

#빌드 결과(JAR) 복사
COPY build/libs/*-SNAPSHOT.jar  app.jar

ENV TZ=Asia/Seoul

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
