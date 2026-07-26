FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 시스템 실행용 non-root 그룹 및 사용자 생성
RUN addgroup --system spring \
    && adduser --system --ingroup spring spring

# 빌드 결과(JAR) 복사하면서 소유권을 spring 사용자/그룹으로 지정
COPY --chown=spring:spring app.jar app.jar

ENV TZ=Asia/Seoul

EXPOSE 8080

# 이후 실행될 명령어가 root가 아닌 spring 권한으로 실행되도록 전환
USER spring

ENTRYPOINT ["java", "-jar", "app.jar"]
