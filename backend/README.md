# ICPC 协会管理系统 · 后端

基于 `docs/ICPC协会管理系统-项目计划书.md` 与 `docs/ICPC协会管理系统-接口文档.yaml` 搭建的 Spring Boot 后端骨架。

## 技术栈

| 分层 | 技术 |
|------|------|
| 运行时 | Java 17+、Spring Boot 3.5 |
| Web | Spring MVC、Bean Validation |
| 安全 | Spring Security + JWT（jjwt 0.12）+ BCrypt |
| 持久层 | MyBatis-Plus 3.5（分页、逻辑删除、自动填充） |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7（JWT 登出黑名单，可降级） |
| 文档 | springdoc-openapi（Swagger UI） |
| 构建 | Maven |

## 目录结构

```
backend/
├─ pom.xml
├─ docker-compose.yml                 # 本地 MySQL + Redis
└─ src/main/
   ├─ java/com/scuec/icpc/
   │  ├─ IcpcSystemApplication.java
   │  ├─ common/
   │  │  ├─ api/                      # ApiResponse / PageResult / PageQuery
   │  │  ├─ config/                   # Security、MyBatis-Plus、OpenAPI、初始化数据
   │  │  ├─ constant/                 # 角色编码与权限层级
   │  │  ├─ entity/                   # BaseEntity 审计字段
   │  │  ├─ enums/                    # 全部业务状态枚举
   │  │  ├─ exception/                # BusinessException + 全局异常处理
   │  │  ├─ log/                      # @AuditLog 操作审计切面
   │  │  └─ security/                 # JWT、UserPrincipal、SecurityUtils
   │  └─ module/
   │     ├─ auth/                     # 登录 / 登出 / 当前用户
   │     ├─ member/                   # 会员 CRUD、入会退会审批、档案
   │     ├─ tag/ problem/             # 知识点、题库、题解、OJ 导入
   │     ├─ training/                 # 训练、提交、榜单、能力雷达
   │     ├─ team/                     # 组队
   │     ├─ camp/                     # 寒假/暑假集训、报名、任务、考勤
   │     ├─ contest/                  # 校赛/XCPC/非 XCPC、报名、榜单、成绩
   │     ├─ mbti/ recommendation/     # 低优先级扩展模块
   │     ├─ statistics/ export/       # 统计与导出
   │     ├─ file/                     # 文件上传
   │     └─ log/                      # 审计日志实体与 Mapper
   └─ resources/
      ├─ application.yml              # 公共配置
      ├─ application-dev.yml          # 开发环境
      ├─ application-prod.yml         # 生产环境
      └─ db/schema.sql                # 完整数据库 DDL
```

## 快速开始

### 1. 启动依赖

```bash
cd backend
docker compose up -d
```

等价于启动 MySQL `localhost:3306`（root/root，库 `icpc_system`）和 Redis `localhost:6379`。
如果本机端口已被占用，可自行调整 `docker-compose.yml` 端口，并通过环境变量覆盖连接配置（见下文）。

### 2. 启动应用

```bash
mvn spring-boot:run
```

首次启动会由 `DataInitializer` 自动写入角色字典、测试账号和 MBTI 题目（`app.init-data=true`，生产环境请关闭）。

### 3. 访问

| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| 上传文件 | http://localhost:8080/uploads/... |

### 4. 测试账号

| 学号 | 密码 | 角色 |
|------|------|------|
| `2023210100` | `Admin@123456` | `PRESIDENT`（拥有全部权限） |
| `2023210101` | `123456` | `MEMBER`（普通会员） |

登录示例：

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"studentId":"2023210100","password":"Admin@123456"}'
```

后续请求携带 `Authorization: Bearer <token>`。

## 配置项

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `DB_URL` | JDBC 连接串 | `jdbc:mysql://localhost:3306/icpc_system?...` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 | `root` / `root` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 | `localhost` / `6379` / 空 |
| `APP_JWT_SECRET` | JWT 密钥（生产必须覆盖，≥32 字符） | 开发用默认值 |
| `CORS_ALLOWED_ORIGINS` | 允许跨域的前端地址，逗号分隔 | `http://localhost:5173,...` |

生产环境启动：

```bash
mvn clean package -DskipTests
java -jar target/icpc-system-1.0.0.jar --spring.profiles.active=prod
```

## 已实现的横切能力

- **统一响应**：`{ code, message, data }`，分页为 `{ total, page, size, records }`。
- **统一异常**：400 / 401 / 403 / 404 / 409 / 422 / 500 与文档错误码一致。
- **JWT 鉴权**：无状态登录，Token 默认 2 小时；登出写入 Redis 黑名单，Redis 不可用时自动降级。
- **RBAC 角色层级**：`ADVISOR > PRESIDENT > MINISTER > ASSISTANT_COACH > OFFICER > TEAM_LEADER > LAB_MEMBER > MEMBER > OBSERVE_MEMBER`，接口使用 `@PreAuthorize("hasRole('...')")` 校验。
- **参数校验**：`@Valid` + 全局异常处理，返回具体字段错误。
- **审计日志**：`@AuditLog("动作")` 自动记录操作人、路径、IP、耗时、状态。
- **逻辑删除**：MyBatis-Plus `@TableLogic`，所有业务表带 `deleted`。
- **自动填充**：`create_time` / `update_time`。
- **Swagger**：自动生成在线接口文档，支持 Bearer Token 调试。
- **文件上传**：按 `yyyy/MM` 分目录存储，返回 URL、原始文件名、大小。

## 接口覆盖

| 模块 | 路径前缀 | 状态 |
|------|----------|------|
| 认证 | `/api/v1/auth` | 登录、登出、当前用户 |
| 会员 | `/api/v1/members` | CRUD、入会/退会申请、审批、完整档案 |
| 知识点 | `/api/v1/tags` | 树查询、增删改 |
| 题库 | `/api/v1/problems` | 分页、详情、增删改、题解、OJ 导入占位 |
| 训练 | `/api/v1/trainings` | CRUD、发布、提交、提交记录、ICPC 榜单、能力评估 |
| 组队 | `/api/v1/teams` | 查询、创建、更新、邀请/移除、教练确认 |
| 集训 | `/api/v1/camps` | 查询、发布、报名、审核、任务、签到、考勤 |
| 赛事 | `/api/v1/contests` | 查询、创建、报名、审核、提交、榜单、成绩、证书 |
| MBTI | `/api/v1/mbti` | 题目、提交、结果 |
| 个性化推荐 | `/api/v1/recommendations` | 题目推荐、训练推荐 |
| 统计 | `/api/v1/statistics` | 会员结构、训练汇总 |
| 导出 | `/api/v1/export` | 训练报表 CSV、证书 ZIP |
| 文件 | `/api/v1/files` | 通用上传 |

## 待接入 / 扩展点

以下位置已预留清晰的扩展点，不影响当前编译与运行：

1. **OJ 判题集成**：`TrainingService#submit`、`ContestService#submit` 目前写入 `PENDING` 提交记录，需接入 `POST /oj/submissions` 并增加定时任务轮询判题结果；`ProblemService#importFromOj` 目前创建占位题目，待接入 `GET /oj/problems?ids=[]`。
2. **证书 PDF 模板**：`ContestService#certificates` 已产出获奖名单，`ExportService` 打包文本证书，可替换为 PDF 模板渲染。
3. **消息通知**：训练/报名结果可接入微信或 QQ 消息推送。
4. **数据权限细化**：当前按角色层级 + 本人/创建者校验，后续可按部门、队伍、集训名单继续细化。
5. **生产部署**：建议补充 Nginx 反向代理、HTTPS、数据库备份、Redis 持久化与日志采集配置。

## 数据库

- 完整 DDL：`src/main/resources/db/schema.sql`，共 25 张表。
- Docker 首次启动会自动执行该脚本。
- 手工初始化：

```bash
mysql -uroot -p < src/main/resources/db/schema.sql
```

## 开发约定

- 包结构：`module/{模块}/controller|service|mapper|entity|dto`。
- DTO 使用嵌套静态类集中管理，避免文件碎片化；Controller 只做参数接收与响应封装，业务写在 Service。
- 写操作尽量加 `@Transactional`，关键操作加 `@AuditLog`。
- 新增接口需同步补充 `@PreAuthorize` 权限声明与 Swagger 注解（可选）。
