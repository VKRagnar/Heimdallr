# 一期后端实施设计与任务拆解

## 1. 实施目标

一期后端围绕“资产可见、监控可查、告警可闭环、通知可追踪、权限可隔离、敏感可控”建设，优先支撑 ACE、iPro、CMS 等核心应用、约 20 台服务器、Kafka、PostgreSQL、Prometheus/Grafana、SkyWalking、日志源和 Agent 接入。

一期不实现完整 CMDB、完整 APM 拓扑、复杂组合规则、动态阈值、智能降噪、完整排班升级静默、工单/事件/IM 正式集成、SQL 深度治理、MQ 消息体治理和自动修复。

## 2. 服务模块

| 模块 | 职责 | 关键表/能力 |
| --- | --- | --- |
| 基础资产服务 | 业务线、部门、应用、负责人、服务器、实例、部署关系 | `business_line`、`application`、`server`、`application_instance` |
| 监控对象与依赖服务 | 统一表达应用、服务器、Kafka、PostgreSQL，维护对象依赖和影响范围 | `monitor_object`、`object_app_dependency`、`data_source_binding` |
| 数据源适配服务 | 封装 Prometheus/Grafana、SkyWalking、ELK/Loki、Agent、Kafka、PostgreSQL 差异 | 健康检查、鉴权、限流、异常码、接入状态 |
| 日志服务 | 日志检索、上下文、错误聚合、脱敏和导出入口 | `log_index_config`、`saved_log_query`、`log_error_signature` |
| 指标服务 | 指标定义、外部映射、指标查询、从指标创建规则 | `metric_definition`、`metric_series_mapping` |
| 告警服务 | 规则、运行态、事件、快照、处理记录、状态机 | `alert_rule`、`alert_event`、`alert_process_record` |
| 通知和值班服务 | 邮件渠道、模板、策略、通知对象解析、失败重试 | `notification_channel`、`notification_record`、`duty_group` |
| 权限与数据范围服务 | 菜单、操作、应用、业务线、环境、敏感字段权限 | `role`、`permission`、`application_authorization` |
| 脱敏导出审计服务 | 脱敏策略、导出任务、审计事件、明文授权 | `mask_policy`、`export_task`、`audit_event` |

## 3. 数据库迁移批次

| 批次 | 内容 | 目标 |
| --- | --- | --- |
| M01 | `dictionary_item`、`business_line`、`department`、`user_account` | 字典、业务线、部门、用户基础 |
| M02 | `application`、`application_owner`、`server`、`application_instance`、`app_server_binding` | 应用、服务器、实例和部署关系 |
| M03 | `monitor_object`、`monitor_object_node`、`object_app_dependency`、`object_dependency_edge`、`data_source`、`data_source_binding`、`agent_instance` | 监控对象、依赖、数据源、Agent |
| M04 | `metric_definition`、`metric_series_mapping`、`metric_query_preset`、`log_index_config`、`saved_log_query`、`log_error_signature` | 指标与日志元数据 |
| M05 | `role`、`permission`、`user_role`、`role_permission`、`application_authorization`、`business_line_authorization`、`sensitive_access_grant` | 权限模型 |
| M06 | `alert_rule`、`alert_rule_target`、`alert_rule_runtime`、`alert_event`、`alert_event_snapshot`、`alert_process_record`、`alert_silence`、`alert_escalation_policy` | 告警规则与事件 |
| M07 | `contact_channel`、`notification_channel`、`notification_template`、`notify_policy`、`notify_policy_target`、`duty_group`、`duty_group_member`、`notification_record` | 通知和值班 |
| M08 | `mask_policy`、`mask_rule`、`export_task`、`audit_event`、`external_event_outbox`、`retention_policy` | 脱敏、导出、审计、外部事件预留 |

初始化数据包括默认角色、默认权限编码、默认脱敏规则、默认邮件模板、Kafka/PostgreSQL 默认规则模板、健康状态、接入状态、数据源状态、Agent 状态、告警状态、规则状态、导出状态和通知状态。

## 4. API 开发顺序

| 阶段 | 优先级 | 接口/任务 | 完成标准 |
| --- | --- | --- | --- |
| A：基础框架与权限底座 | P0 | `/me`、`/me/data-scope`、审计基础、统一响应、错误码、权限拦截器 | 任意业务接口可拿到用户、业务线、应用、环境、操作权限 |
| B：资产与接入配置 | P0 | 应用、服务器、实例、依赖、数据源、Agent | ACE/iPro/CMS 可建立资产、负责人、服务器、实例、绑定 |
| C：指标与日志查询 | P0 | `/metrics/definitions`、`/metrics/query`、`/logs/search`、日志上下文、错误聚合 | 指标和日志按应用授权过滤，敏感字段默认脱敏 |
| D：告警规则配置 | P0 | 规则 CRUD、启用校验、从指标创建规则、默认模板 | 未接入、无数据、数据源异常对象不可启用规则 |
| E：告警引擎与状态机 | P0 | 规则调度、持续时间、去重、恢复、处理动作 | 检测延迟不超过 1 分钟，同一维度未恢复前只存在一个活跃事件 |
| F：邮件通知和值班 | P0 | 邮件渠道测试、模板渲染、接收人解析、通知记录 | 告警触发后可发送邮件或记录失败原因 |
| G：导出、脱敏、明文查看 | P0/P1 | 导出任务、脱敏策略、敏感明文授权、下载审计 | 导出异步化，明文查看有理由、授权、有效期和审计 |
| H：首页聚合 | P1 | `/home/summary`、`/home/pending-issues` | 只统计授权范围内应用、服务器、告警和接入风险 |

## 5. 告警引擎

检测流程：

```text
读取启用规则
-> 过滤生效时间
-> 校验数据源和指标状态
-> 按检测周期生成任务
-> 分片/加锁领取任务
-> 查询指标数据
-> 写检测样本
-> 判断是否满足触发条件
-> 判断是否达到持续时间
-> 生成或更新告警事件
-> 进入通知编排
```

去重键：

```text
dedup_key = rule_id + object_id + metric_code + dimension_hash
```

失败补偿：

| 场景 | 处理 |
| --- | --- |
| 数据源超时 | 记录检测失败，不生成业务告警 |
| 鉴权失败 | 规则进入配置异常，通知维护人或平台管理员 |
| 指标无数据 | 标记数据不足，连续超过阈值后配置异常 |
| 通知失败 | 按邮件重试策略重试，最终进入 `notification_failed` |
| 事件写入失败 | 检测任务重试，依赖 `dedup_key` 幂等 |
| 审计写入失败 | 高风险操作失败或进入待审计补偿队列 |

## 6. 权限校验

```text
最终可访问范围 =
  菜单权限
  ∩ 操作权限
  ∩ 应用授权范围
  ∩ 业务线范围
  ∩ 环境约束
  ∩ 敏感字段权限
```

后端必须过滤搜索、下拉、列表、统计卡片、详情页、聚合数量、导出、URL 直访、告警规则目标选择、日志和指标查询、通知和值班解析、审计查询。

## 7. 接口联调顺序

1. 基础权限与资产：`/me`、`/me/data-scope`、应用、服务器、审计。
2. 接入管理与数据源：数据源、验证、Agent、应用依赖。
3. 指标与日志：指标定义、指标查询、日志搜索、上下文、错误聚合。
4. 规则配置：告警规则列表、创建、编辑、启用、从指标创建。
5. 告警事件与处理：告警列表、详情、确认、处理中、备注、关闭。
6. 通知和值班：通知记录、邮件渠道测试、值班组。
7. 导出、审计、敏感明文：导出任务、审计查询、明文授权边界。
8. 首页聚合与全链路：首页摘要、待处理问题、上下文跳转。
