# 企业级统一监控平台详细设计目录

## 1. 文档定位

本目录承接 `architecture/technical-architecture-design.md`，用于沉淀技术架构之后的详细设计产物。

## 2. 文档清单

| 文档 | 定位 | 状态 |
| --- | --- | --- |
| `database-er-design.md` | PostgreSQL 业务库表结构、ER、索引、约束和保留策略 | 已完成 |
| `core-api-permission-design.md` | 核心 API、请求响应、错误码和权限校验 | 已完成 |
| `data-source-adapter-design.md` | Prometheus/Grafana、SkyWalking、日志源、Agent、Kafka、PostgreSQL 接入适配 | 已完成 |
| `alert-engine-state-machine-design.md` | 告警规则引擎、检测调度、状态机、默认规则模板 | 已完成 |
| `notification-audit-masking-design.md` | 邮件通知、审计、导出、脱敏和敏感明文查看 | 已完成 |

## 3. 编制原则

1. 详细设计必须承接已确认的一期范围：ACE、iPro、CMS、约 20 台服务器、Kafka、PostgreSQL、邮件通知、多业务线独立管理。
2. 所有接口、表结构和流程必须体现应用授权、业务线隔离、敏感字段脱敏、导出审计和接入状态区分。
3. 未接入、采集异常、业务异常必须在模型和接口中分开展示。
4. 一期不实现工单、事件系统、IM 机器人正式集成，但事件模型和外部扩展字段需要预留。
