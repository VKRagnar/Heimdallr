# 一期前端实施设计与页面开发排期

## 1. 实施目标

一期前端围绕“看见状态、定位问题、触发通知、基础权限隔离”落地，优先服务应用研发负责人，其次服务运维/SRE、值班人员和平台管理员。

一期不新增“中间件监控”“数据库监控”一级导航，Kafka、PostgreSQL 等对象通过应用详情依赖、指标中心、告警中心和接入管理承载。

## 2. 页面模块

| 一级模块 | 页面/视图 | 核心内容 | 优先级 |
| --- | --- | --- | --- |
| 全局框架 | 顶部栏、左侧导航、面包屑、来源提示条 | 菜单权限、全局搜索、环境、刷新状态、用户菜单、上下文返回 | P0 |
| 首页总览 | 排障视角首页 | 健康摘要、待处理问题、P0/P1 告警、异常应用、慢接口、数据接入异常 | P0 |
| 应用监控 | 应用列表、应用详情 | 应用状态、实例、接口、依赖、告警、接入状态、日志/指标入口 | P0 |
| 服务器监控 | 服务器列表、服务器详情 | 主机/IP、资源指标、Agent、部署应用、当前告警、进程端口 | P0/P1 |
| 日志中心 | 日志查询 | 应用/实例/级别/Trace ID/时间查询、上下文、错误聚合、脱敏、导出 | P0 |
| 指标中心 | 指标查询/指标趋势 | 指标筛选、趋势图、阈值线、告警触发点、创建规则入口 | P0 |
| 告警中心 | 告警列表、详情、规则 | 级别、状态、对象、负责人、通知状态、处理动作、规则配置 | P0 |
| 通知与值班 | 通知记录、联系人、值班组、邮件渠道 | 发送状态、失败原因、重试、邮件渠道测试、模板预留 | P0/P1 |
| 接入管理 | 应用接入、数据源、Agent | 资产、负责人、实例、数据源绑定、接入验证、Agent 状态 | P0 |
| 系统管理 | 用户、角色、权限、审计 | 菜单权限、操作权限、应用授权、敏感权限、审计查询 | P0 |
| 导出任务 | 任务列表/详情抽屉 | 后台导出进度、失败原因、下载有效期、审计编号 | P1 |
| 通用状态页 | 空、加载、异常、无权限、未接入、超时 | 统一文案、重试、申请权限、接入指引 | P0 |

## 3. 路由设计

| 路由 | 页面 | 关键 Query/State |
| --- | --- | --- |
| `/home` | 首页总览 | `env`、`businessLineId`、`ownerId`、`timeRange` |
| `/applications` | 应用列表 | `keyword`、`env`、`status`、`ownerId`、`accessStatus` |
| `/applications/:appId` | 应用详情 | `env`、`tab`、`instanceId`、`interfacePath`、`from`、`backUrl` |
| `/servers` | 服务器列表 | `keyword`、`env`、`agentStatus`、`appId`、`status` |
| `/servers/:serverId` | 服务器详情 | `env`、`tab`、`timeRange`、`backUrl` |
| `/logs/search` | 日志查询 | `appId`、`env`、`instanceId`、`traceId`、`level`、`start`、`end`、`alertId` |
| `/metrics` | 指标查询 | `objectType`、`objectId`、`metricCodes`、`env`、`timeRange` |
| `/alerts` | 告警列表 | `severity`、`status`、`objectType`、`appId`、`ownerId`、`timeRange` |
| `/alerts/:alertId` | 告警详情 | `from`、`backUrl`、`tab` |
| `/alert-rules` | 告警规则列表 | `objectType`、`appId`、`status`、`severity` |
| `/alert-rules/new` | 新建规则 | `fromMetric`、`objectType`、`objectId`、`metricCode` |
| `/notifications` | 通知记录 | `alertId`、`channel`、`sendStatus`、`receiverId`、`timeRange` |
| `/access/applications` | 应用接入 | `appId`、`env`、`accessStatus` |
| `/access/data-sources` | 数据源管理 | `sourceType`、`status`、`env` |
| `/access/agents` | Agent 管理 | `hostname`、`env`、`agentStatus` |
| `/system/audit-events` | 审计日志 | `eventType`、`operatorId`、`objectType`、`timeRange` |
| `/export-tasks` | 导出任务 | `status`、`sourcePage`、`createdBy`、`timeRange` |

## 4. 状态与组件

| 能力 | 实施要求 |
| --- | --- |
| 当前用户与权限 | 全局缓存 `/api/v1/me`、`/api/v1/me/data-scope`，控制菜单、按钮、数据范围提示 |
| 路由上下文 | URL Query 保存对象、环境、时间窗口、来源页、返回筛选，刷新后恢复 |
| 页面筛选 | 列表/查询页筛选进入 URL，支持保存查询、重置、返回保持 |
| 自动刷新 | 首页、告警列表、告警详情支持自动刷新，不清空筛选、排序、展开行、抽屉 |
| 权限失败 | `FORBIDDEN`、`MENU_FORBIDDEN`、`APP_DATA_FORBIDDEN`、`ENV_FORBIDDEN`、`SENSITIVE_FIELD_FORBIDDEN` 映射不同 UI |
| 导出任务 | 创建后进入后台任务轮询，成功、失败、过期、无权限均有状态提示 |

复用组件包括 `AppShell`、`PageHeader`、`SourceContextBar`、`PermissionGate`、`FilterBar`、`DataTable`、`StatusTag`、`MetricChartCard`、`AlertActionBar`、`LogResultTable`、`ErrorAggregationPanel`、`AuditHint`、`ExportConfirm`、`ConfigDrawer`。

## 5. 权限可见性

| 角色 | 默认可见 | 默认可操作 | 受控/不可见 |
| --- | --- | --- | --- |
| 平台管理员 | 全部一期菜单、全部应用、全部环境、审计 | 用户、角色、权限、数据源、通知渠道、审计、全局配置 | 敏感日志/密钥明文仍需敏感权限 |
| 运维/SRE | 首页、应用、服务器、日志、指标、告警、通知、接入 | 数据源、规则、告警处理、通知记录、导出 | 权限变更需管理员授权 |
| 应用负责人 | 授权应用、实例、日志、指标、告警、关联服务器摘要、依赖摘要 | 查看详情、查日志、处理本应用告警、创建本应用规则 | 非授权应用、全局配置、生产敏感字段 |
| 测试人员 | 测试/预发授权应用、日志、指标、实例状态 | 查询、查看、保存个人查询 | 生产数据、规则修改、导出 |
| 值班人员 | 值班组覆盖告警、关联对象、触发窗口日志/指标 | 确认、处理中、备注、基础转派 | 非值班范围、系统配置、全量历史 |
| 只读用户 | 授权范围内看板、列表、详情 | 查看、筛选、复制非敏感 ID | 导出、处理、配置、明文 |

## 6. 开发顺序

| 阶段 | 开发内容 | 目标 |
| --- | --- | --- |
| 第 1 阶段：前端基建 | AppShell、路由、权限模型、API Client、错误码映射、状态标签、FilterBar、DataTable、通用状态页 | 建立复用骨架 |
| 第 2 阶段：资产与权限闭环 | 登录用户信息、菜单权限、应用列表、应用详情基础、应用授权、系统用户/角色基础 | 打通应用授权主线 |
| 第 3 阶段：核心排障链路 | 首页、告警列表、告警详情、日志查询、指标趋势、应用详情 Tab 联动 | 打通首页/告警 -> 日志/指标 -> 应用详情 |
| 第 4 阶段：告警配置与通知 | 告警规则、规则启停、通知记录、邮件渠道测试、值班组基础、联系人 | 支撑告警闭环 |
| 第 5 阶段：接入管理 | 应用接入、数据源管理、Agent 管理、接入状态、验证中/采集异常/未接入 | 支撑真实接入 |
| 第 6 阶段：服务器与依赖对象 | 服务器列表/详情、应用依赖 Tab、Kafka/PostgreSQL 摘要、对象指标入口 | 补齐基础设施视角 |
| 第 7 阶段：导出、审计与体验完善 | 导出确认、导出任务、审计日志、响应式、可访问性、边界状态 | 完成验收闭环 |

## 7. 联调计划

联调按 `/me` 权限、资产接入、日志指标、告警规则、通知和值班、接入管理、导出审计、端到端演练 8 批推进。端到端演练链路为：首页 -> 告警详情 -> 日志 -> 应用详情 -> 指标 -> 处理记录，试点数据为 ACE/iPro/CMS、20 台服务器、Kafka、PostgreSQL。
