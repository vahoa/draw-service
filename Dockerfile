# ============================================
# 抽奖服务 - Docker构建文件
# 支持通过环境变量指定运行环境 (dev/test/prod)
# ============================================

FROM eclipse-temurin:25-jdk-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制Maven配置
COPY pom.xml .
COPY src ./src

# 构建应用（默认使用dev环境，可通过--build-arg覆盖）
ARG SPRING_PROFILE=dev
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests -P${SPRING_PROFILE} || \
    (apk add --no-cache maven && mvn clean package -DskipTests -P${SPRING_PROFILE})

# 运行阶段
FROM eclipse-temurin:25-jre-alpine

# 安装必要的工具
RUN apk add --no-cache curl

# 创建应用目录
WORKDIR /app

# 创建日志目录
RUN mkdir -p /var/log/draw-service

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令 - 通过SPRING_PROFILES_ACTIVE环境变量指定运行环境
# 优先级：docker run -e SPRING_PROFILES_ACTIVE=prod > Dockerfile ENV > 默认值dev
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -jar app.jar"]
