# 抽奖服务 - 多环境配置说明

## 配置文件结构

```
src/main/resources/
├── application.yml          # 主配置（公共配置，所有环境共享）
├── application-dev.yml      # 开发环境配置
├── application-test.yml     # 测试环境配置
└── application-prod.yml     # 生产环境配置
```

## 配置说明

### 主配置 (application.yml)
包含所有环境的公共配置：
- 服务端口、Tomcat线程配置
- Nacos服务发现和配置中心
- Sentinel限流配置
- 数据源、Redis、MongoDB、Kafka连接配置
- 业务配置（防刷、抽奖、库存）
- 日志和Actuator配置

### 环境特定配置

**开发环境 (application-dev.yml)**
- 日志级别为DEBUG，便于调试
- 使用默认的本地连接配置

**测试环境 (application-test.yml)**
- 日志级别为INFO
- 连接测试环境的Nacos分组

**生产环境 (application-prod.yml)**
- 日志级别为WARN，减少日志输出
- 数据库连接池优化（更大的连接数）
- 日志输出到文件（/var/log/draw-service/）
- 连接生产环境的Nacos分组

---

## 使用方式

### 方式一：Maven打包指定环境

```bash
# 开发环境（默认）
mvn clean package

# 测试环境
mvn clean package -Ptest

# 生产环境
mvn clean package -Pprod
```

### 方式二：运行时指定环境

```bash
# 通过命令行参数
java -jar draw-service.jar --spring.profiles.active=prod

# 或者通过环境变量
SPRING_PROFILES_ACTIVE=prod java -jar draw-service.jar
```

### 方式三：Docker运行指定环境

```bash
# 构建时指定环境
docker build --build-arg SPRING_PROFILE=prod -t draw-service:prod .

# 运行时通过环境变量覆盖
docker run -e SPRING_PROFILES_ACTIVE=prod -p 8080:8080 draw-service:prod
```

### 方式四：Docker Compose

```bash
# 开发环境
docker-compose --profile dev up draw-service-dev

# 测试环境
docker-compose --profile test up draw-service-test

# 生产环境（需要先配置.env文件）
cp .env.example .env
# 编辑 .env 文件填入实际配置
docker-compose --profile prod up draw-service-prod
```

---

## 环境变量配置

### 核心环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 运行环境 (dev/test/prod) | dev |
| `NACOS_SERVER_ADDR` | Nacos服务器地址 | localhost:8848 |
| `NACOS_NAMESPACE` | Nacos命名空间 | 空 |
| `NACOS_USERNAME` | Nacos用户名 | 空 |
| `NACOS_PASSWORD` | Nacos密码 | 空 |
| `MYSQL_URL` | MySQL连接URL | localhost:3306 |
| `MYSQL_USERNAME` | MySQL用户名 | root |
| `MYSQL_PASSWORD` | MySQL密码 | root123 |
| `REDIS_CLUSTER_NODES` | Redis集群节点 | localhost:7000-7005 |
| `REDIS_PASSWORD` | Redis密码 | 空 |
| `KAFKA_SERVERS` | Kafka服务器地址 | localhost:9092 |
| `MONGODB_URI` | MongoDB连接URI | localhost:27017 |
| `SENTINEL_DASHBOARD` | Sentinel控制台地址 | localhost:8080 |
| `REGION` | 区域标识 | default |

---

## 快速开始

### 本地开发

```bash
# 默认使用dev环境
mvn spring-boot:run

# 或者指定环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 生产部署

```bash
# 1. 打包生产环境版本
mvn clean package -Pprod

# 2. 构建Docker镜像
docker build --build-arg SPRING_PROFILE=prod -t draw-service:prod .

# 3. 准备环境变量文件
cp .env.example .env
# 编辑 .env 填入生产环境配置

# 4. 运行容器
docker run -d \
  --name draw-service \
  -p 8080:8080 \
  --env-file .env \
  -e SPRING_PROFILES_ACTIVE=prod \
  draw-service:prod
```

---

## 注意事项

1. **配置优先级**：命令行参数 > 环境变量 > 配置文件
2. **生产环境**：务必修改默认密码，使用强密码
3. **Nacos配置**：生产环境建议配置命名空间进行环境隔离
4. **日志文件**：生产环境日志会写入 `/var/log/draw-service/`，确保目录可写
