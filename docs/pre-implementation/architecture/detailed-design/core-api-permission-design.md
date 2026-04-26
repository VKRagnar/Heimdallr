# 核心 API 与权限校验详细设计

## 1. API 约定

一期 API 统一使用 `/api/v1` 前缀，资源名使用复数。查询使用 `GET`，创建使用 `POST`，修改使用 `PUT` 或 `PATCH`，停用、关闭、确认等状态变化使用动作端点，不做物理删除。导出统一通过 `/api/v1/export-tasks` 异步处理。

请求头：

| Header | 说明 |
| --- | --- |
| `Authorization` | 登录令牌 |
| `X-Request-Id` | 请求链路 ID |
| `X-Client-Timezone` | 客户端时区 |
| `Content-Type` | 默认 `application/json` |

统一响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "requestId": "req-20260426-0001",
  "timestamp": "2026-04-26T16:00:00+08:00"
}
```

分页响应统一返回 `items`、`pageNo`、`pageSize`、`total`。

## 2. 权限模型

最终可访问范围：

```text
菜单权限
∩ 操作权限
∩ 应用授权范围
∩ 业务线范围
∩ 环境约束
∩ 敏感字段权限
```

应用负责人以应用授权为主线查看应用、服务器、日志、指标、告警和依赖对象。业务线管理员可管理所属业务线内对象。平台管理员可跨业务线配置，但敏感明文仍需单独授权或二次确认。

| 权限维度 | 说明 |
| --- | --- |
| 菜单权限 | 控制模块入口，如应用监控、日志、告警、配置管理 |
| 操作权限 | 控制新增、编辑、启停、导出、关闭、授权等动作 |
| 应用授权 | 控制应用及其关联实例、日志、指标、告警 |
| 业务线范围 | 控制多业务线独立管理和看板隔离 |
| 环境约束 | 生产、测试、预发等环境可独立授权 |
| 敏感字段权限 | SQL、Token、连接串、个人信息明文查看 |

## 3. 核心端点

| 模块 | 端点 | 说明 |
| --- | --- | --- |
| 认证与个人信息 | `GET /api/v1/me` | 当前用户、角色、业务线、菜单 |
| 认证与个人信息 | `GET /api/v1/me/data-scope` | 当前用户可访问应用、业务线和环境范围 |
| 首页 | `GET /api/v1/home/summary` | 应用、服务器、告警、采集状态摘要 |
| 首页 | `GET /api/v1/home/pending-issues` | 待处理告警和采集异常 |
| 应用 | `GET /api/v1/applications` | 应用列表，按业务线、环境、名称过滤 |
| 应用 | `GET /api/v1/applications/{appId}` | 应用详情 |
| 应用 | `GET /api/v1/applications/{appId}/instances` | 应用实例 |
| 应用 | `GET /api/v1/applications/{appId}/interfaces` | 接口和链路入口 |
| 应用 | `GET /api/v1/applications/{appId}/dependencies` | 依赖 Kafka、PostgreSQL 等对象 |
| 服务器 | `GET /api/v1/servers` | 服务器列表 |
| 服务器 | `GET /api/v1/servers/{serverId}/metrics` | CPU、内存、磁盘、网络 |
| 服务器 | `GET /api/v1/servers/{serverId}/processes` | 进程 |
| 服务器 | `GET /api/v1/servers/{serverId}/ports` | 端口 |
| 日志 | `POST /api/v1/logs/search` | 日志检索 |
| 日志 | `GET /api/v1/logs/{logId}/context` | 上下文日志 |
| 日志 | `POST /api/v1/logs/error-aggregations` | 错误聚合 |
| 指标 | `GET /api/v1/metrics/definitions` | 指标定义 |
| 指标 | `POST /api/v1/metrics/query` | 指标查询 |
| 指标 | `POST /api/v1/alert-rules/from-metric` | 从指标创建规则 |
| 告警 | `GET /api/v1/alerts` | 告警列表 |
| 告警 | `GET /api/v1/alerts/{alertId}` | 告警详情 |
| 告警 | `POST /api/v1/alerts/{alertId}/acknowledge` | 确认 |
| 告警 | `POST /api/v1/alerts/{alertId}/mark-processing` | 标记处理中 |
| 告警 | `POST /api/v1/alerts/{alertId}/comments` | 添加备注 |
| 告警 | `POST /api/v1/alerts/{alertId}/transfer` | 转派 |
| 告警 | `POST /api/v1/alerts/{alertId}/close` | 关闭 |
| 规则 | `GET /api/v1/alert-rules` | 规则列表 |
| 规则 | `POST /api/v1/alert-rules` | 新建规则 |
| 规则 | `PATCH /api/v1/alert-rules/{ruleId}` | 修改规则 |
| 规则 | `POST /api/v1/alert-rules/{ruleId}/enable` | 启用 |
| 通知和值班 | `GET /api/v1/notifications` | 通知记录 |
| 通知和值班 | `POST /api/v1/notification-channels/{channelId}/test` | 渠道测试 |
| 通知和值班 | `GET /api/v1/duty-groups` | 值班组 |
| 接入配置 | `GET /api/v1/data-sources` | 数据源 |
| 接入配置 | `POST /api/v1/data-sources/{sourceId}/validate` | 连通性校验 |
| 接入配置 | `GET /api/v1/agents` | Agent 列表和在线状态 |
| 权限系统 | `GET /api/v1/access/users` | 用户 |
| 权限系统 | `GET /api/v1/access/roles` | 角色 |
| 权限系统 | `POST /api/v1/access/application-authorizations` | 应用授权 |
| 系统 | `GET /api/v1/system/audit-events` | 审计查询 |
| 导出 | `POST /api/v1/export-tasks` | 创建导出任务 |
| 导出 | `GET /api/v1/export-tasks/{taskId}` | 导出状态 |
| 扩展 | `POST /api/v1/webhooks/events` | 二期工单、事件、IM 扩展入口 |

## 4. 权限校验规则

| 场景 | 校验 |
| --- | --- |
| 查看应用 | 用户拥有应用授权，或拥有业务线管理权限，或为平台管理员 |
| 查看服务器 | 服务器绑定授权应用，或用户是服务器负责人，或拥有业务线/平台权限 |
| 查看 Kafka/PostgreSQL | 只能查看授权应用依赖的对象摘要；全局指标需要对象负责人或 SRE 权限 |
| 日志检索 | 必须有应用日志权限；敏感字段按脱敏策略处理 |
| 指标查询 | 必须有应用或对象指标权限；共享对象按依赖关系裁剪 |
| 告警处理 | 必须能访问告警关联应用或对象，并具备对应操作权限 |
| 规则启停 | 需要规则配置权限、目标对象权限、通知策略权限 |
| 导出 | 需要导出权限、数据范围权限、脱敏策略和审计记录 |
| 明文查看 | 需要敏感字段权限、临时授权或二次确认，并写审计 |

## 5. 错误码

| 错误码 | 说明 |
| --- | --- |
| `BAD_REQUEST` | 请求格式错误 |
| `UNAUTHORIZED` | 未登录 |
| `TOKEN_EXPIRED` | 登录过期 |
| `FORBIDDEN` | 无权限 |
| `MENU_FORBIDDEN` | 无菜单权限 |
| `APP_DATA_FORBIDDEN` | 无应用数据权限 |
| `ENV_FORBIDDEN` | 无环境权限 |
| `SENSITIVE_FIELD_FORBIDDEN` | 无敏感字段权限 |
| `NOT_FOUND` | 资源不存在 |
| `CONFLICT` | 状态冲突 |
| `VALIDATION_FAILED` | 参数校验失败 |
| `RATE_LIMITED` | 请求限流 |
| `INTERNAL_ERROR` | 系统异常 |
| `APPLICATION_NOT_AUTHORIZED` | 应用未授权 |
| `DATA_SOURCE_UNAVAILABLE` | 数据源不可用 |
| `METRIC_NOT_FOUND` | 指标不存在 |
| `METRIC_NO_RECENT_DATA` | 指标近期无数据 |
| `LOG_QUERY_TIMEOUT` | 日志查询超时 |
| `ALERT_STATUS_CONFLICT` | 告警状态不允许当前动作 |
| `ALERT_RULE_INVALID` | 告警规则配置非法 |
| `CHANNEL_TEST_FAILED` | 通知渠道测试失败 |
| `EXPORT_TASK_TOO_LARGE` | 导出范围过大 |
| `SECRET_VIEW_FORBIDDEN` | 密钥或敏感明文禁止查看 |

## 6. 一期优先接口

P0 接口包括首页摘要、应用列表和详情、日志检索、指标查询、告警列表和处理、规则配置、邮件通知记录、数据源校验、应用授权、审计查询和导出任务。P1 接口包括依赖拓扑、保存查询、错误聚合、值班组、Agent 在线状态、敏感明文授权和 Webhook 扩展入口。
