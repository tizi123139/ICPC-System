# ICPC 协会管理系统 · API 接口手册

> **文档编号**：ICPC-API-V1.0
> **编制日期**：2026-10-31
> **基础路径**：`/api/v1`
> **完整 OpenAPI 规范**：见同目录下 `ICPC协会管理系统-接口文档.yaml`，可导入 Swagger Editor / Apifox 生成在线调试页面

---

## 0 通用规范

### 0.1 鉴权

| 项 | 说明 |
|----|------|
| 方式 | JWT Bearer Token |
| 获取 | 调用 `POST /auth/login` 成功后返回 `token` 字段 |
| 传递 | 后续所有请求头加 `Authorization: Bearer <token>` |
| 过期 | 默认 2 小时；过期返回 `401`，需重新登录 |

### 0.2 通用响应格式

所有接口统一返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | `0` = 成功；`400` 参数错误；`401` 未登录；`403` 无权限；`404` 资源不存在；`500` 服务异常 |
| `message` | string | 人类可读提示信息 |
| `data` | any / null | 业务数据；列表/分页接口结构见下方 |

### 0.3 分页响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 156,
    "page": 1,
    "size": 20,
    "records": [ ... ]
  }
}
```

### 0.4 通用请求头

| Header | 值 | 说明 |
|--------|-----|------|
| `Authorization` | `Bearer <token>` | 除登录接口外必填 |
| `Content-Type` | `application/json` | POST/PUT 请求体格式 |

### 0.5 角色编码速查

| 编码 | 角色 | 权限简述 |
|------|------|---------|
| `OBSERVE_MEMBER` | 观察会员 | 可提交入会申请、参加日常训练、查看公开题库 |
| `MEMBER` | 正式会员 | 同上 + 组队、校赛/XCPC 报名 |
| `LAB_MEMBER` | 竞赛实验室成员 | 同上 + 可看集训私有题库 |
| `TEAM_LEADER` | 队长 | 同上 + 管理队伍成员 |
| `OFFICER` | 干事 | 审核报名、考勤、数据统计 |
| `ASSISTANT_COACH` | 助理教练 | 出题、创建训练、考核、集训任务发布 |
| `MINISTER` | 部长/副部长 | 审批申请、奖惩、考核结果 |
| `PRESIDENT` | 会长/副会长 | 系统管理、最高审批 |
| `ADVISOR` | 指导老师 | 只读监督 + 重大决策审批 |

### 0.6 业务状态枚举速查

**会员状态**：`ACTIVE` 活跃 / `INACTIVE` 不活跃 / `RESIGNED` 已退会 / `REMOVED` 已除名

**申请状态**：`PENDING` 待审核 / `APPROVED` 通过 / `REJECTED` 驳回

**训练状态**：`DRAFT` 草稿 / `PUBLISHED` 已发布 / `ONGOING` 进行中 / `ENDED` 已结束

**判题结果**：`PENDING` 等待中 / `JUDGING` 评测中 / `AC` 正确 / `WA` 答案错误 / `TLE` 超时 / `MLE` 超内存 / `RE` 运行错误 / `CE` 编译错误

**赛事分类**：`SCHOLASTIC` 校赛 / `XCPC` XCPC 系列 / `NON_XCPC` 非 XCPC 校外赛

**集训类型**：`WINTER` 寒假 / `SUMMER` 暑假

---

## 1 认证模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/auth/login` | 用户登录（学号 + 密码） | 公开 |
| POST | `/auth/logout` | 用户登出 | 已登录 |
| GET | `/auth/profile` | 获取当前登录用户信息 | 已登录 |

### 登录 `POST /auth/login`

请求体：

```json
{
  "studentId": "2023210100",
  "password": "******"
}
```

响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200,
    "user": {
      "userId": 1,
      "studentId": "2023210100",
      "name": "张三",
      "roles": ["MEMBER"],
      "avatar": null
    }
  }
}
```

---

## 2 会员管理

### 2.1 会员信息 CRUD

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/members` | 分页查询会员列表（支持多条件筛选） | 已登录 |
| POST | `/members` | 新增会员 | `OFFICER` / `MINISTER` / `PRESIDENT` |
| GET | `/members/{memberId}` | 获取会员详情 | 已登录 |
| PUT | `/members/{memberId}` | 更新会员信息 | `OFFICER` / `MINISTER` / `PRESIDENT` |
| DELETE | `/members/{memberId}` | 逻辑删除会员 | `PRESIDENT` |
| GET | `/members/{memberId}/archive` | 获取会员完整档案 | 本人 / `OFFICER`+ |

**列表查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 默认 1 |
| `size` | int | 否 | 默认 20 |
| `keyword` | string | 否 | 姓名/学号模糊搜索 |
| `memberType` | enum | 否 | `OBSERVE_MEMBER` / `MEMBER` / `LAB_MEMBER` |
| `grade` | string | 否 | 如 "2023级" |
| `department` | string | 否 | 部门 |
| `status` | enum | 否 | `ACTIVE` / `INACTIVE` / `RESIGNED` / `REMOVED` |

### 2.2 入会 / 退会审批

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/members/apply/join` | 提交入会申请 | 公开（观察期前） |
| POST | `/members/apply/resign` | 提交退会申请 | 已登录会员 |
| GET | `/members/approvals` | 查询待审批申请列表 | `OFFICER`+ |
| POST | `/members/approvals/{applyId}/audit` | 审批申请 | `OFFICER`+（退会除名需 `MINISTER`+） |

**审批请求体**：

```json
{
  "action": "APPROVE",
  "remark": "基础扎实，通过入会审核"
}
```

---

## 3 知识点与题库管理

### 3.1 知识点管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/tags` | 获取知识点树（含父子层级） | 已登录 |
| POST | `/tags` | 新增知识点 | `ASSISTANT_COACH`+ |
| PUT | `/tags/{tagId}` | 修改知识点 | `ASSISTANT_COACH`+ |
| DELETE | `/tags/{tagId}` | 删除知识点 | `MINISTER`+ |

### 3.2 题目管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/problems` | 分页查询题目（支持知识点/难度/可见范围筛选） | 已登录 |
| POST | `/problems` | 新增题目 | `ASSISTANT_COACH`+ |
| GET | `/problems/{problemId}` | 获取题目详情 | 已登录（集训私有题需 `LAB_MEMBER`+） |
| PUT | `/problems/{problemId}` | 修改题目 | `ASSISTANT_COACH`+ |
| DELETE | `/problems/{problemId}` | 删除题目 | `MINISTER`+ |
| PUT | `/problems/{problemId}/solution` | 更新题解（Markdown + 参考代码） | `ASSISTANT_COACH`+ |
| POST | `/problems/import` | 从 OJ 批量导入题目 | `ASSISTANT_COACH`+ |

**新增题目请求体**：

```json
{
  "title": "A + B Problem",
  "ojProblemId": "1000",
  "difficulty": "EASY",
  "timeLimit": 1000,
  "memoryLimit": 131072,
  "description": "...Markdown 格式...",
  "inputFormat": "两个整数 A B",
  "outputFormat": "一个整数 A+B",
  "sampleInput": "1 2",
  "sampleOutput": "3",
  "hint": null,
  "tagIds": [1, 3],
  "visibility": "PUBLIC"
}
```

---

## 4 训练管理

### 4.1 训练 CRUD

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/trainings` | 分页查询训练列表 | 已登录 |
| POST | `/trainings` | 创建训练（单人/组队） | `ASSISTANT_COACH`+ |
| GET | `/trainings/{trainingId}` | 获取训练详情（含题目列表） | 已登录 |
| PUT | `/trainings/{trainingId}` | 修改训练（仅 `DRAFT` 状态） | 训练创建者 |
| DELETE | `/trainings/{trainingId}` | 删除训练 | 训练创建者 / `MINISTER`+ |
| POST | `/trainings/{trainingId}/publish` | 发布训练 | 训练创建者 |

**创建训练请求体**：

```json
{
  "name": "第十周队内训练赛",
  "type": "SOLO",
  "startTime": "2026-11-15T19:00:00+08:00",
  "endTime": "2026-11-15T22:00:00+08:00",
  "problemIds": [100, 101, 102, 103, 104],
  "leaderboardVisible": true,
  "freezeTime": 30
}
```

### 4.2 提交与判题

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/trainings/{trainingId}/submit` | 提交代码 | 已登录会员（训练开放时段内） |
| GET | `/trainings/{trainingId}/submissions` | 查询提交记录 | 本人 / `ASSISTANT_COACH`+ 可查全体 |

**提交代码请求体**：

```json
{
  "problemId": 100,
  "code": "#include <iostream>\nusing namespace std;\nint main() { int a,b; cin>>a>>b; cout<<a+b; }",
  "language": "C++17"
}
```

**提交响应**：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "submitId": 5001,
    "result": "AC"
  }
}
```

> 判题通过自建 OJ 异步完成，客户端可轮询 `/trainings/{trainingId}/submissions` 查看最终状态。

### 4.3 榜单与能力评估

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/trainings/{trainingId}/leaderboard` | 实时榜单（自动识别封榜状态） | 已登录 |
| GET | `/trainings/ability/{userId}` | 个人能力雷达图数据 + 文字报告 | 本人 / `ASSISTANT_COACH`+ |

**榜单响应示例**：

```json
{
  "code": 0,
  "data": {
    "frozen": false,
    "entries": [
      { "rank": 1, "userId": 3, "userName": "张三", "solvedCount": 5, "penalty": 125 },
      { "rank": 2, "userId": 7, "userName": "李四", "solvedCount": 4, "penalty": 98 }
    ]
  }
}
```

---

## 5 组队管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/teams` | 查询队伍列表 | 已登录 |
| POST | `/teams` | 创建队伍（发起组队申请） | `MEMBER`+ |
| GET | `/teams/{teamId}` | 队伍详情（含成员、能力画像） | 已登录 |
| PUT | `/teams/{teamId}` | 更新队伍信息 | 队长 |
| POST | `/teams/{teamId}/members` | 邀请成员 | 队长 |
| DELETE | `/teams/{teamId}/members?memberId=` | 移除成员 / 离队 | 队长 / 本人 |
| POST | `/teams/{teamId}/confirm` | 教练确认队伍 | `ASSISTANT_COACH`+ |

---

## 6 集训管理（寒假 / 暑假共用接口）

> 集训报名、审核、任务、考勤接口结构完全一致，通过 `type` 区分寒假（`WINTER`）和暑假（`SUMMER`）。

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/camps` | 查询集训列表（`type=WINTER` 或 `type=SUMMER`） | 已登录 |
| POST | `/camps` | 发布集训通知 | `ASSISTANT_COACH`+ |
| GET | `/camps/{campId}` | 集训详情（含任务列表） | 已登录 |
| POST | `/camps/{campId}/register` | 提交集训报名 | 已登录会员 |
| GET | `/camps/{campId}/registrations` | 报名列表 | `ASSISTANT_COACH`+ |
| POST | `/camps/{campId}/registrations/{applyId}/audit` | 审核报名 | `ASSISTANT_COACH`+ |
| POST | `/camps/{campId}/tasks` | 发布集训每日任务 | `ASSISTANT_COACH`+ |
| POST | `/camps/{campId}/checkin` | 签到打卡 | 参训名单内成员 |
| GET | `/camps/{campId}/attendance` | 考勤记录查询 | 本人 / `ASSISTANT_COACH`+ |
| PUT | `/camps/{campId}/attendance/{recordId}` | 补签 / 请假 / 标记缺勤 | `ASSISTANT_COACH`+ |

**签到请求体**：

```json
{
  "date": "2027-01-15",
  "remark": ""
}
```

---

## 7 赛事管理（校赛 / XCPC / 非 XCPC 共用基础）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/contests` | 查询赛事列表（支持 `category` 筛选） | 已登录 |
| POST | `/contests` | 创建/发布赛事 | `ASSISTANT_COACH`+ |
| GET | `/contests/{contestId}` | 赛事详情 | 已登录 |
| POST | `/contests/{contestId}/register` | 队伍/个人报名 | 符合条件的会员 |
| GET | `/contests/{contestId}/registrations` | 报名列表 | 赛事组织者 / `ASSISTANT_COACH`+ |
| POST | `/contests/{contestId}/registrations/{applyId}/audit` | 审核报名 | `ASSISTANT_COACH`+ |
| GET | `/contests/{contestId}/leaderboard` | 实时榜单（自动封榜） | 已登录 |
| POST | `/contests/{contestId}/submit` | 比赛中提交代码 | 已报名队伍成员 |
| POST | `/contests/{contestId}/certificates` | 批量生成获奖电子证书 | `MINISTER`+ |
| PUT | `/contests/{contestId}/results` | 赛后登记获奖/成绩 | `ASSISTANT_COACH`+ |

**发布赛事请求体**：

```json
{
  "name": "中南民族大学第十届校内赛",
  "category": "SCHOLASTIC",
  "startTime": "2026-12-06T14:00:00+08:00",
  "endTime": "2026-12-06T19:00:00+08:00",
  "registrationDeadline": "2026-12-04T23:59:59+08:00",
  "description": "面向全校的年度校内程序设计竞赛...",
  "maxTeamSize": 3,
  "problemIds": [200, 201, 202, 203, 204, 205, 206, 207],
  "freezeMinutes": 60
}
```

---

## 8 MBTI 管理（低优先级）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/mbti/questions` | 获取测评题目列表 | 已登录 |
| POST | `/mbti/submit` | 提交答案，返回测评结果 | 已登录 |
| GET | `/mbti/results/{userId}` | 查看某会员测评结果 | 本人 / `ASSISTANT_COACH`+ |

---

## 9 个性化训练推荐（低优先级）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/recommendations/problems` | 基于薄弱知识点推荐习题 | 已登录 |
| GET | `/recommendations/trainings` | 基于能力评估推荐专项训练 | 已登录 |

---

## 10 数据统计与导出

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/statistics/members/overview` | 会员结构统计（年级/专业/部门/类型） | `OFFICER`+ |
| GET | `/statistics/training/summary` | 训练数据汇总 | `ASSISTANT_COACH`+ |
| GET | `/export/training-report/{trainingId}` | 导出训练成绩报表（Excel） | `ASSISTANT_COACH`+ |
| GET | `/export/contest-certificates/{contestId}` | 下载赛事获奖证书 ZIP | 本人 / `MINISTER`+ |

---

## 11 文件上传

| 方法 | 路径 | 说明 | 权限 | Content-Type |
|------|------|------|------|--------------|
| POST | `/files/upload` | 通用文件上传（题解附件、证书、证明材料等） | 已登录 | `multipart/form-data` |

响应：

```json
{
  "code": 0,
  "data": {
    "url": "/uploads/2026/10/abc123.pdf",
    "filename": "入会动机说明.pdf",
    "size": 245760
  }
}
```

---

## 附录 A 接口清单汇总（按模块）

| 模块 | 接口数量 | 高优先级 | 中优先级 | 低优先级 |
|------|---------|---------|---------|---------|
| 认证 | 3 | 3 | 0 | 0 |
| 会员管理 | 11 | 10 | 1 | 0 |
| 题库管理 | 9 | 7 | 2 | 0 |
| 训练管理 | 9 | 9 | 0 | 0 |
| 组队管理 | 7 | 7 | 0 | 0 |
| 集训（寒/暑） | 10 × 2 | 16 | 4 | 0 |
| 赛事（校赛/XCPC/非XCPC） | 10 × 3 | 24 | 3 | 3 |
| MBTI | 3 | 0 | 0 | 3 |
| 个性化推荐 | 2 | 0 | 0 | 2 |
| 统计与导出 | 4 | 1 | 2 | 1 |
| 文件上传 | 1 | 1 | 0 | 0 |
| **合计** | **~73** | **~68** | **~12** | **~9** |

## 附录 B 错误码速查

| 码 | HTTP 状态 | 说明 | 典型 message |
|----|----------|------|-------------|
| 0 | 200 | 成功 | ok |
| 400 | 400 | 参数错误 | "参数校验失败：xxx 不能为空" |
| 401 | 401 | 未登录 / Token 过期 | "未授权，请先登录" |
| 403 | 403 | 无权限 | "无权执行此操作" |
| 404 | 404 | 资源不存在 | "会员不存在" |
| 409 | 409 | 状态冲突 | "训练已结束，无法提交代码" |
| 422 | 422 | 业务逻辑不满足 | "队伍人数不满足 XCPC 赛事要求" |
| 500 | 500 | 服务内部错误 | "服务器异常" |

## 附录 C 与 OJ 系统集成接口（外部依赖）

| 方向 | OJ 接口 | 本系统用途 |
|------|---------|-----------|
| 拉取 | `GET /oj/problems?ids=[]` | 从 OJ 同步题目基础信息 |
| 拉取 | `GET /oj/problems/{id}` | 获取题目详情用于题库维护 |
| 推送 | `POST /oj/submissions` | 提交训练/比赛代码到 OJ 判题 |
| 拉取 | `GET /oj/submissions/{id}/result` | 轮询判题结果 |
| 拉取 | `GET /oj/problems/{id}/stats` | 题目通过率统计 |

> OJ 集成接口协议需在阶段一与协会开发部共同评审确认。
