# 企业级统一监控平台开发排期与实施设计目录

## 1. 文档定位

本目录承接 `architecture/technical-architecture-design.md` 和 `architecture/detailed-design/`，用于沉淀技术评审通过后的开发排期、实施拆解、联调验收和上线准备。

## 2. 阶段结论

技术架构设计已经评审通过，可以进入开发排期与实施设计阶段。一期实施范围保持为 ACE、iPro、CMS 等核心应用，约 20 台服务器，Kafka、PostgreSQL，邮件通知，多业务线独立管理，应用授权，日志/指标/告警/审计/导出/脱敏闭环。

## 3. 文档清单

| 文档 | 定位 | 状态 |
| --- | --- | --- |
| `development-schedule.md` | 开发阶段划分、Sprint 排期、里程碑、依赖和风险缓冲 | 已完成 |
| `backend-implementation-design.md` | 后端服务模块、数据库迁移、API、告警引擎、通知审计导出脱敏任务拆解 | 已完成 |
| `frontend-implementation-design.md` | 前端页面、路由、状态、组件、权限可见性和联调计划 | 已完成 |
| `data-access-ops-implementation.md` | ACE/iPro/CMS、服务器、Kafka、PostgreSQL、Prometheus/日志/Agent/SkyWalking 接入实施 | 已完成 |
| `testing-release-risk-plan.md` | 测试验收、UAT、上线检查、回滚和风险控制 | 已完成 |

## 4. 编制原则

1. 排期以一期可交付闭环为准，优先完成应用监控、日志、指标、告警、邮件通知、权限和审计。
2. 数据接入先覆盖 ACE、iPro、CMS 和核心服务器，再扩展 Kafka、PostgreSQL 和业务指标。
3. 每个阶段必须具备可演示、可验收、可回滚的交付物。
4. 权限、脱敏、导出、审计、接入状态区分必须从第一轮联调开始验证。
5. 工单、事件系统、IM 机器人、复杂告警策略和统一 CMDB 对接保留扩展，不阻塞一期上线。
