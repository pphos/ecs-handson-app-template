# ========================================
# Stage 1: Build
# ========================================
FROM public.ecr.aws/docker/library/gradle:9.2.0-jdk-25-and-25-corretto AS builder

WORKDIR /app

# Gradle Wrapper + ビルド設定
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# 依存関係キャッシュ
RUN ./gradlew dependencies --no-daemon || true

# ソースコピー & ビルド
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# レイヤー展開 (重要!)
RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# ========================================
# Stage 2: Runtime
# ========================================
FROM public.ecr.aws/amazoncorretto/amazoncorretto:25.0.1
WORKDIR /app

# レイヤードJARを順番にコピー (変更頻度の低い順)
COPY --from=builder --chown=spring:spring /app/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/application/ ./

USER 1000

EXPOSE 8080

# コンテナ最適化されたJVMオプション
ENV JAVA_OPTS="\
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+ExitOnOutOfMemoryError"

# レイヤードJAR用のエントリーポイント
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]