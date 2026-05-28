# Heimdallr Data Monitor

## Project Progress Handoff

Before starting work in a new session, read `project/PROJECT_PROGRESS.md`. Keep that file updated with current progress, verification evidence, blockers, and next actions.

## Agent Team Workflow

This repository uses a focused Agent team workflow for software delivery: Architecture -> Exploration -> Implementation -> Testing -> Review.

- Start with `AGENTS.md` for repository-level Agent rules.
- Use `project/AGENT_TEAM.md` for the full role model, handoff template, routing rules, and verification gates.

Heimdallr Data Monitor 是一个企业级统一监控平台，当前覆盖应用资产、服务器资产、用户权限、审计事件和基础可观测能力。仓库包含前端控制台、后端 API、多模块领域代码、数据库迁移脚本、部署依赖和前期产品/架构设计文档。

## 当前状态

- 后端：Spring Boot 4 + Java 25 + Maven 多模块，当前使用内存数据实现 Sprint 1 API 验证。
- 前端：React 19 + Vite + Ant Design + React Query，开发环境在 API 不可用或 404 时会回退到 mock 数据。
- 基础设施：提供本地 PostgreSQL 17 和 Redis 8 的 Docker Compose 配置。
- 数据库：`backend/db-migration/` 已包含 Sprint 1 表结构和种子数据 SQL。
- 文档：`docs/pre-implementation/` 下包含需求、原型、架构、详细设计、实施计划和评审材料。

## 目录结构

```text
.
├── backend/                 # Maven 多模块后端工程
│   ├── common-domain/       # 领域模型、统一响应、领域异常
│   ├── common-observability/# RequestId 等可观测基础能力
│   ├── common-security/     # Token 鉴权、当前用户上下文
│   ├── db-migration/        # Sprint 1 数据库迁移和种子数据
│   └── heimdallr-api/       # Spring Boot API 应用
├── frontend/                # Vite + React 前端控制台
├── deploy/                  # 本地依赖编排配置
├── scripts/import-samples/  # 应用、服务器、依赖导入样例
└── docs/                    # 产品、设计、架构与实施文档
```

## 环境要求

- JDK 25
- Maven 3.9+
- Node.js 20+
- npm 10+
- Docker / Docker Compose

## 本地启动

### 1. 启动本地依赖

```powershell
Copy-Item deploy\.env.example deploy\.env
docker compose --env-file deploy\.env -f deploy\docker-compose.yml up -d
```

默认端口：

- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

### 2. 启动后端 API

```powershell
cd backend
mvn spring-boot:run -pl heimdallr-api -am
```

后端默认监听 `http://localhost:8080`。

健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/health
```

### 3. 启动前端控制台

```powershell
cd frontend
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`，并通过 Vite proxy 将 `/api` 转发到 `http://localhost:8080`。

如需指定 API 地址：

```powershell
$env:VITE_API_BASE_URL="http://localhost:8080"
npm run dev
```

## 认证与测试 Token

除 `/health` 和 `/actuator/**` 外，后端接口需要 `Authorization` 请求头。当前内置了 3 个演示 Token：

| Token | 用户 | 角色 | 说明 |
| --- | --- | --- | --- |
| `admin-token` | `platform-admin` | `PLATFORM_ADMIN` | 全局数据范围，拥有应用、服务器、审计和访问权限 |
| `sre-token` | `sre` | `SRE` | 可查看部分业务线和环境，拥有应用、服务器、审计权限 |
| `ace-owner-token` | `ace-owner` | `APP_OWNER` | 仅可查看 ACE 应用相关数据 |

示例：

```powershell
Invoke-RestMethod `
  -Headers @{ Authorization = "Bearer admin-token"; "X-Request-Id" = "req-local-readme" } `
  http://localhost:8080/api/v1/me
```

前端会从 `localStorage` 的 `heimdallr-token` 读取 Token。浏览器控制台可临时设置：

```js
localStorage.setItem('heimdallr-token', 'admin-token');
location.reload();
```

## 常用 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/health` | 应用健康检查 |
| `GET` | `/api/v1/me` | 当前用户信息 |
| `GET` | `/api/v1/me/data-scope` | 当前用户数据范围 |
| `GET` | `/api/v1/applications` | 应用资产列表 |
| `GET` | `/api/v1/applications/{id}` | 应用详情 |
| `GET` | `/api/v1/applications/{id}/instances` | 应用实例列表 |
| `GET` | `/api/v1/servers` | 服务器资产列表 |
| `GET` | `/api/v1/access/users` | 用户列表 |
| `GET` | `/api/v1/access/roles` | 角色列表 |
| `GET` | `/api/v1/system/audit-events` | 审计事件列表 |

> 注意：前端当前仍有部分系统管理服务调用使用 mock fallback，开发调试时以接口表和后端 controller 为准。

## 构建与测试

后端：

```powershell
cd backend
mvn test
```

前端：

```powershell
cd frontend
npm run lint
npm run test
npm run build
```

## 数据库脚本

Sprint 1 数据库脚本位于：

- `backend/db-migration/V001__sprint1_foundation.sql`
- `backend/db-migration/V002__sprint1_seed_data.sql`

当前 API 使用内存数据夹具，数据库脚本用于后续持久化实现、迁移验证和样例数据对齐。

## 导入样例

`scripts/import-samples/` 提供了第一批资产和依赖样例：

- `applications.csv`：ACE、IPRO、CMS 应用实例清单
- `servers.csv`：服务器、Agent、访问方式和部署应用字段
- `dependencies.json`：Kafka、PostgreSQL 依赖样例

这些样例可用于后续导入 API、集成测试和数据库种子数据校验。

## 相关文档

- 需求文档：`docs/pre-implementation/demand/monitoring-platform-requirements.md`
- 架构目录：`docs/pre-implementation/architecture/README.md`
- 实施计划：`docs/pre-implementation/implementation-plan/README.md`
- UI 设计：`docs/pre-implementation/ui-design/README.md`
- 产品原型：`docs/pre-implementation/product-prototype/README.md`
