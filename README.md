# ICPC-System

中南民族大学计算机学院 ICPC 协会管理系统。

## 项目文档

- [项目计划书](docs/ICPC协会管理系统-项目计划书.md)
- [接口手册](docs/ICPC协会管理系统-接口手册.md)
- [OpenAPI 规范](docs/ICPC协会管理系统-接口文档.yaml)
- [软件需求规格说明书](docs/软件需求规格说明书.docx)

## 代码结构

```
ICPC-System/
├─ docs/       # 需求、计划、接口文档
└─ backend/    # Spring Boot 3 后端
```

## 后端启动

```bash
cd backend
docker compose up -d      # MySQL 8 + Redis 7
mvn spring-boot:run
```

- API 基础路径：`http://localhost:8080/api/v1`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- 管理员账号：`2023210100 / Admin@123456`

详细说明见 [backend/README.md](backend/README.md)。
