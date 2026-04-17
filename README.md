# LuckDraw · 幸运抽大奖

> 基于 **DDD 领域驱动设计** 的高并发企业级在线抽奖系统
> 技术栈：Spring Boot 4 · Spring Cloud Alibaba · Vue 3 · TypeScript · MyBatis-Flex · Redisson

---

## 目录

- [界面预览](#界面预览)
- [功能特性](#功能特性)
- [项目结构](#项目结构)
- [DDD架构分层](#ddd架构分层)
- [核心业务流程](#核心业务流程)
- [奖品池设计](#奖品池设计)
- [抽奖机制](#抽奖机制)
- [技术实现要点](#技术实现要点)
- [API接口文档](#api接口文档)
- [运营策略分析](#运营策略分析)
- [数据库设计](#数据库设计)
- [快速启动](#快速启动)

---

## 界面预览

```
┌─────────────────────────────────────────────────────────┐
│  幸运抽大奖   🔊 恭喜用户M**，抽中3积分          我的奖品 >│
│                                                         │
│  ◀ [会员] [3积分] [1积分] [华为Mate X7] [大疆] [华为手表] ▶│
│                              ↑ 当前高亮（动画滚动选中）   │
│                                                         │
│                    ┌──────────────┐                     │
│                    │   立即抽奖   │                     │
│                    └──────────────┘                     │
│                       剩余 3 次                         │
└─────────────────────────────────────────────────────────┘
```

**界面核心组件**

| 组件 | 描述 |
|------|------|
| 标题栏 | "幸运抽大奖" + 跑马灯式中奖滚动通知 |
| 奖品展示区 | 横向老虎机滑动卡片，GSAP驱动抽奖动画 |
| 抽奖按钮 | 渐变红色按钮，含剩余次数提示 |
| 中奖弹窗 | 模态框展示奖品，含粒子动画特效 |
| 操作引导 | "去查看"按钮引导用户进入奖品页 |

---

## 功能特性

- ✅ **横向老虎机抽奖动画** — GSAP 实现加速→匀速→减速滚动效果
- ✅ **概率权重算法** — 别名方法（Alias Method）O(1) 时间复杂度抽奖
- ✅ **分布式锁防并发** — Redisson 保障高并发下抽奖原子性
- ✅ **乐观锁库存扣减** — 数据库行级乐观锁，防止超发
- ✅ **每日次数重置** — 按天重置用户抽奖次数
- ✅ **虚拟奖品即时发放** — 积分/会员自动发放，实物奖品创建物流工单
- ✅ **跑马灯通知** — 实时展示其他用户中奖信息，营造氛围
- ✅ **防刷机制** — 用户维度 + IP维度频率限制
- ✅ **DDD 架构** — 领域层/应用层/接口层/基础设施层清晰分离

---

## 项目结构

```
LuckDraw/
├── draw-service/          # 后端服务（Spring Boot）
│   └── src/main/java/cn/vahoa/draw/
│       ├── domain/        # 领域层（核心业务）
│       │   ├── entity/    # 领域实体
│       │   ├── repository/# 仓储接口
│       │   ├── service/   # 领域服务（抽奖算法、核心逻辑）
│       │   └── valueobject/
│       ├── application/   # 应用层（用例编排）
│       │   ├── service/   # 应用服务
│       │   └── dto/       # 应用层DTO
│       ├── interfaces/    # 接口层（对外暴露）
│       │   ├── controller/# REST Controller
│       │   ├── dto/       # 接口层DTO
│       │   └── mq/        # 消息队列消费者
│       ├── infrastructure/# 基础设施层
│       │   ├── cache/     # Redis缓存
│       │   ├── mq/        # Kafka消息
│       │   ├── repository/# 仓储实现
│       │   └── security/  # 安全配置
│       └── common/        # 公共组件（Result、异常等）
│
├── lucky-spin/            # 前端（Vue 3 + TypeScript）
│   └── src/
│       ├── views/         # 页面组件
│       │   └── LotteryView.vue   # 抽奖主页面
│       ├── api/           # API调用层
│       │   └── lottery.ts
│       ├── router/        # 路由
│       ├── utils/         # 工具（请求封装等）
│       └── styles/        # 全局样式
│
├── database/              # 数据库脚本
├── deploy/                # 部署配置（K8s yaml）
├── docker/                # Docker配置
└── docs/                  # 项目文档
```

---

## DDD架构分层

```
┌────────────────────────────────────────────────────────┐
│                   interfaces（接口层）                  │
│   DrawController  ←  HTTP请求  →  Result<DrawResult>   │
└────────────────────────┬───────────────────────────────┘
                         │ 调用
┌────────────────────────▼───────────────────────────────┐
│                  application（应用层）                  │
│   DrawAppService  ←  用例编排，DTO转换，通知发送        │
└────────────────────────┬───────────────────────────────┘
                         │ 调用
┌────────────────────────▼───────────────────────────────┐
│                    domain（领域层）                     │
│  LotteryService ← 核心业务：加锁、算法、库存、发放      │
│  LotteryAlgorithm ← 别名方法抽奖算法                    │
│  Prize / LotteryRecord / UserLotteryQuota（实体）       │
│  PrizeRepository / LotteryRecordRepository（仓储接口）  │
└────────────────────────┬───────────────────────────────┘
                         │ 实现
┌────────────────────────▼───────────────────────────────┐
│                infrastructure（基础设施层）              │
│   MyBatis-Flex 实现仓储  |  Redisson分布式锁             │
│   Redis缓存  |  Kafka消息队列  |  Nacos配置中心           │
└────────────────────────────────────────────────────────┘
```

---

## 核心业务流程

### 抽奖主流程

```
用户点击"立即抽奖"
       │
       ▼
前端启动老虎机滚动动画（GSAP）
       │
       ▼（异步并发）
调用 POST /api/v1/draw/execute
       │
       ▼
【后端：DrawController】
       │
       ▼
【后端：DrawAppService】
       │
       ▼
【后端：LotteryService — 核心领域服务】
       │
       ├─ 1. 验证用户资格（登录状态 + 剩余次数检查）
       │
       ├─ 2. 获取可用奖品列表（状态=启用 且 有库存）
       │
       ├─ 3. 获取 Redisson 分布式锁（防止同一用户并发）
       │
       ├─ 4. 双重检查（加锁后再次检查次数）
       │
       ├─ 5. 扣减抽奖次数（数据库原子操作）
       │
       ├─ 6. 执行抽奖算法（AliasMethod，O(1)）
       │
       ├─ 7. 扣减奖品库存（乐观锁，库存不足降级为谢谢参与）
       │
       ├─ 8. 持久化抽奖记录（lottery_record）
       │
       ├─ 9. 发放奖品
       │       ├─ 积分/会员 → 直接发放（同步调用）
       │       └─ 实物 → 创建物流工单（异步 Kafka）
       │
       └─ 10. 更新奖品今日发放数量
       │
       ▼
返回 DrawResult（recordNo / prizeName / prizeType / remaining）
       │
       ▼
前端动画至少播放 3 秒后停止
       │
       ▼
弹出中奖结果弹窗（含奖品图片 + 粒子动画）
       │
       ▼
用户点击"去查看" → 跳转我的奖品页
```

---

## 奖品池设计

### 奖品类型分层

```
奖品池
├── 实物大奖（type=3）     概率极低，库存有限
│   ├── 华为折叠屏 Mate X7   0.1%   库存 5
│   ├── 大疆相机 Pocket3     0.5%   库存 20
│   ├── 华为手表 GT6         1.0%   库存 50
│   └── 华为蓝耳机 FreeBuds  2.0%   库存 100
│
├── 虚拟中奖（type=2）     中等概率，库存较大
│   └── i会员（月卡）        5.0%   库存 10000
│
└── 普惠奖（type=1）       高概率，库存无限
    ├── 3积分               30.0%  无限库存
    └── 1积分               40.0%  无限库存
    （谢谢参与）            21.4%  无限库存
                           ──────
                    合计：100%
```

### 库存保护机制

```
实物奖品抽中 → 乐观锁扣减库存
        │
     成功 ──→ 正常发放
        │
     失败（库存为0）──→ 降级为"谢谢参与"
```

---

## 抽奖机制

### 别名方法算法（Alias Method）

工程中采用 `AliasMethodLotteryAlgorithm`，时间复杂度 **O(1)**，预处理 O(n)。

- 适合高频抽奖场景，预处理一次可重复使用
- 构建别名表缓存（`ConcurrentHashMap`），奖品配置不变时无需重建
- 支持任意概率分布，无需整数化概率

```
传入奖品列表 prizes
       │
归一化概率 → 构建别名表（prob[] + alias[]）
       │
       ▼
随机选列 column = random(0, n)
随机抛硬币 coin = random(0, 1)
       │
coin < prob[column]  →  返回 prizes[column]
coin >= prob[column] →  返回 prizes[alias[column]]
```

### 分布式并发控制

```
用户A 发起抽奖           用户A重复点击
      │                       │
      ▼                       ▼
 尝试获取锁               尝试获取锁
(等待最多3s)             (等待最多3s)
      │                       │
   获取成功                超时失败
      │                       │
   执行抽奖              返回"操作频繁"
      │
   释放锁
```

---

## 技术实现要点

### 后端

```java
// 核心抽奖流程（LotteryService.draw）
@Transactional(rollbackFor = Exception.class)
public LotteryRecord draw(String userId, Long poolId, ...) {
    // 1. 验证次数
    UserLotteryQuota quota = getUserQuota(userId, poolId);
    
    // 2. Redisson 分布式锁
    RLock lock = redissonClient.getLock("lottery:draw:" + userId);
    lock.tryLock(3, 10, TimeUnit.SECONDS);
    
    // 3. 双重检查 + 扣减次数
    userLotteryQuotaRepository.useQuota(userId, poolId);
    
    // 4. Alias Method 抽奖
    Prize prize = lotteryAlgorithm.draw(availablePrizes);
    
    // 5. 乐观锁扣减库存
    prizeRepository.deductStock(prize.getId());
    
    // 6. 持久化记录 + 发放奖品
    lotteryRecordRepository.insert(record);
}
```

### 前端

```typescript
// 老虎机动画 + API调用（LotteryView.vue）
async function startDraw() {
    playDrawAnimation()          // GSAP 驱动滚动动画
    
    const result = await lotteryApi.executeDraw({ userId, poolId })
    
    await delay(Math.max(0, 3000 - elapsed))  // 保证动画至少3秒
    
    if (result.success) {
        showModal.value = true   // 展示中奖弹窗
    }
}

function playDrawAnimation() {
    // 加速阶段 → 匀速阶段 → 减速阶段
    gsap.timeline()
        .to({}, { duration: 0.05 })  // 加速
        .to({}, { duration: 0.10 })  // 匀速
        .to({}, { duration: 0.20 })  // 减速
}
```

---

## API接口文档

### GET `/api/v1/draw/info` — 获取抽奖页面信息

**请求参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | String | 用户ID |
| poolId | Long | 奖池ID |

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "prizes": [
      { "id": 1, "name": "i会员", "type": 2, "imageUrl": "/images/prize/vip.png", "value": "30" },
      { "id": 2, "name": "3积分", "type": 1, "imageUrl": "/images/prize/coin-3.png", "value": "3" }
    ],
    "remainingQuota": 3,
    "totalQuota": 3,
    "usedQuota": 0,
    "recentWinNotices": [
      "恭喜用户us**r1抽中华为手表GT6",
      "恭喜用户us**r2抽中i会员"
    ]
  }
}
```

---

### POST `/api/v1/draw/execute` — 执行抽奖

**请求体**

```json
{
  "userId": "user_abc123",
  "poolId": 1
}
```

**返回示例（中奖）**

```json
{
  "code": 200,
  "data": {
    "success": true,
    "recordNo": "DR17135892340001",
    "prizeId": 4,
    "prizeName": "华为折叠屏Mate X7",
    "prizeType": 3,
    "prizeValue": "华为Mate X7",
    "win": true,
    "message": "恭喜您获得 华为折叠屏Mate X7！",
    "remainingQuota": 2
  }
}
```

**返回示例（失败）**

```json
{
  "code": 500,
  "message": "今日抽奖次数已用完"
}
```

---

## 运营策略分析

| 策略 | 实现方式 | 目的 |
|------|----------|------|
| 普惠小奖（积分） | 高概率积分奖品，库存无限 | 保证用户每次都有收获感，提升留存 |
| 实物大奖展示 | 前端展示华为手机等大奖卡片 | 制造期待感，刺激参与 |
| 实时滚动通知 | 从 `lottery_record` 查最近中奖记录 | 利用从众心理，营造热闹氛围 |
| "去查看"引导 | 中奖弹窗跳转奖品页 | 将抽奖流量引导至其他业务页面 |
| 剩余次数显示 | 实时展示"剩余 N 次" | 制造紧迫感，降低流失 |
| 用户ID脱敏 | 展示"us**r1"格式 | 保护隐私，同时增加真实感 |

---

## 数据库设计

### 核心表结构

```sql
-- 奖品表
prize (id, name, type, value, image_url, probability, stock, daily_limit, today_sent, sort_order, status)

-- 用户抽奖次数表（按用户+奖池维度，每日重置）
user_lottery_quota (id, user_id, pool_id, total_quota, used_quota, free_quota, reset_time)

-- 抽奖记录表
lottery_record (id, record_no, user_id, pool_id, prize_id, prize_name, prize_type, prize_value, status, draw_time, grant_time, client_ip, device_id)
```

### 防超发设计

```sql
-- 乐观锁扣减库存（stock <= -1 为无限库存，不扣减）
UPDATE prize
SET stock = stock - 1, updated_at = NOW()
WHERE id = #{prizeId}
  AND (stock > 0 OR stock = -1)

-- 原子扣减抽奖次数（条件：已用次数 < 总次数）
UPDATE user_lottery_quota
SET used_quota = used_quota + 1, updated_at = NOW()
WHERE user_id = #{userId}
  AND pool_id = #{poolId}
  AND used_quota < total_quota
```

---

## 快速启动

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 25 |
| Spring Boot | 4.0.5 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 7.0+（集群模式） |
| Kafka | 4.1.1（KRaft 模式） |
| Nacos | 2.x |

### 后端启动

```bash
# 开发环境
mvn spring-boot:run

# 指定环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 打包
mvn clean package -Pprod
```

### 前端启动

```bash
cd lucky-spin
npm install
npm run dev
```

### Docker 启动

```bash
# 开发环境
docker-compose --profile dev up

# 生产环境
cp .env.example .env
# 编辑 .env 填入生产配置
docker-compose --profile prod up -d
```

---

## 配置说明

多环境配置文件结构：

```
src/main/resources/
├── application.yml        # 主配置（公共）
├── application-dev.yml    # 开发环境（DEBUG 日志）
├── application-test.yml   # 测试环境
└── application-prod.yml   # 生产环境（连接池优化，日志写文件）
```

通过环境变量切换：

```bash
SPRING_PROFILES_ACTIVE=prod java -jar draw-service.jar
```

---

> 作者：vahoa · 版本：1.0.0-SNAPSHOT · 构建时间：2026
