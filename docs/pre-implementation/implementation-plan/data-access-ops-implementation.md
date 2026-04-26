# 数据接入与运维实施设计

## 1. 一期接入范围

| 类别 | 范围 | 一期目标 |
| --- | --- | --- |
| 核心应用 | ACE、iPro、CMS | 建立应用资产、负责人、环境、实例、日志、指标、告警和接入状态 |
| 服务器 | 约 20 台服务器 | 接入基础资源、Agent 状态、部署关系和服务器告警 |
| 中间件 | Kafka | 接入 Broker、Topic、Consumer Group 指标和默认告警 |
| 数据库 | PostgreSQL | 接入实例、库、连接、慢 SQL、锁、复制、容量指标和默认告警 |
| 数据源 | Prometheus/Grafana、日志源、少量 SkyWalking、Agent | 纳入数据源管理、接入状态和健康检查 |
| 通知 | 邮件 | 告警触发后发送邮件，记录通知结果和失败原因 |

一期不新增“中间件监控”“数据库监控”一级导航，中间件和数据库通过应用详情依赖、指标中心、告警中心、接入管理承载。

## 2. 接入顺序

| 阶段 | 接入对象 | 目标 | 验收口径 |
| --- | --- | --- | --- |
| 第 0 阶段 | 平台基础环境 | PostgreSQL、数据源管理、权限、邮件、自监控可用 | 平台能登录、能配置数据源、能发送测试邮件 |
| 第 1 阶段 | ACE | 作为首个灰度应用 | 应用资产、日志、指标、告警、负责人、邮件通知闭环跑通 |
| 第 2 阶段 | iPro | 复用 ACE 模板扩展 | 接入配置可复制，权限、日志、指标、告警正常 |
| 第 3 阶段 | CMS | 完成三大核心应用覆盖 | ACE/iPro/CMS 均可在首页、应用、日志、指标、告警中展示 |
| 第 4 阶段 | 20 台服务器 | 补齐部署关系和 Agent 状态 | 服务器资源、Agent 心跳、部署应用关系可见 |
| 第 5 阶段 | Kafka | 接入消息队列依赖 | Topic/Consumer Group 与应用依赖关系可见，默认规则可启用 |
| 第 6 阶段 | PostgreSQL | 接入数据库依赖 | 连接、慢 SQL、锁、容量、复制延迟指标和规则可用 |
| 第 7 阶段 | 全量巡检 | 灰度扩大与运维验收 | 未接入、采集异常、业务异常分开展示，无误告警 |

ACE 放在第一位验证完整链路；iPro 第二验证模板复用；CMS 第三验证多应用、多业务线和权限隔离。

## 3. 环境准备

| 项目 | 要求 |
| --- | --- |
| 网络 | 平台服务可访问 Prometheus/Grafana、日志源、SkyWalking、Kafka Exporter、PostgreSQL Exporter、SMTP |
| 账号 | 准备 Prometheus 查询账号、日志源查询账号、SkyWalking 查询账号、PostgreSQL 只读采集账号 |
| PostgreSQL 业务库 | 保存资产、权限、规则、事件、通知、审计、脱敏、接入配置等管理数据 |
| 指标存储 | 复用 Prometheus/VictoriaMetrics/Thanos，不将指标明细写入平台业务库 |
| 日志存储 | 复用 OpenSearch/Elasticsearch/Loki，不将日志明细写入平台业务库 |
| 邮件 | 配置 SMTP 主通道，建议预留备用 SMTP |
| 时间同步 | 平台、服务器、Prometheus、日志源、Exporter 时间需同步 |
| 安全 | Token、密码、连接串只保存密钥引用，不保存明文 |

20 台服务器需整理 `hostname`、`ip`、`env`、`business_line`、`owner`、`apps`、`agent_status`、`os_type`、`region/zone`、`tags`。Agent 心跳建议 30-60 秒一次，超过阈值标记无心跳。

## 4. 数据源接入步骤

| 数据源 | 步骤 | 验收要求 |
| --- | --- | --- |
| Prometheus/Grafana | 登记数据源、配置地址/认证/超时/限流、测试查询、同步指标和标签、配置映射、绑定 Grafana Dashboard | 查询成功，目标对象有近期采集数据；异常时不生成“指标为 0”的业务误告警 |
| 日志源 | 登记 ELK/Loki、配置索引模式和字段映射、配置保留周期、启用脱敏、验证按应用/实例/级别/Trace ID 查询 | 日志默认至少保留 1 个月，低频应用可配置 1 年；告警跳日志带触发前 10 分钟至后 20 分钟 |
| SkyWalking | 登记数据源、映射 Service/Instance、拉取接口请求量/错误率/P95/P99、支持 Trace ID 跳转 | 一期只做链路入口和接口摘要，不要求完整 APM 拓扑 |
| Agent | 安装 Agent、注册主机信息、上报版本和心跳、展示采集配置版本 | 状态包括正常、无心跳、版本过低、配置异常、未安装 |
| Kafka | 部署/复用 Exporter，接入 Prometheus，登记集群/Broker/Topic/Consumer Group，建立应用依赖，启用默认规则 | 应用负责人只看授权应用相关 Topic/Consumer Group |
| PostgreSQL | 创建最小权限只读采集账号，部署 Exporter，登记实例/集群/库/副本，建立应用依赖，启用默认规则 | SQL 文本、库表名、会话明细、连接串默认脱敏 |

## 5. 配置模板

应用资产模板：

```yaml
application:
  code: ACE
  name: ACE
  env: prod
  business_line: logistics
  department: platform
  owners:
    primary: user001
    backup: user002
  tech_stack: java
  health_check_url: ""
  access_status: verifying
  log_enabled: true
  metric_enabled: true
  trace_enabled: false
```

数据源模板：

```yaml
data_source:
  source_name: prometheus-prod
  source_type: Prometheus
  env: prod
  base_url: https://prometheus.example.com
  health_check_path: /api/v1/query
  auth_type: token
  secret_ref: secret/prometheus-prod-token
  timeout_seconds: 5
  retry_count: 2
  rate_limit_qps: 20
```

Kafka/PostgreSQL 通过 `monitor_object` 建模，并使用 `dependencies` 记录与 ACE/iPro/CMS 的 Topic、Consumer Group、数据库依赖关系。

## 6. 灰度策略

| 阶段 | 范围 | 放量条件 | 观察指标 |
| --- | --- | --- | --- |
| 灰度 1 | ACE 单应用、少量服务器 | 日志、指标、告警、邮件闭环正常 | 数据源成功率、误告警、查询耗时 |
| 灰度 2 | ACE 全实例 + iPro 部分实例 | 模板可复用，权限无越权 | 应用权限、日志脱敏、规则准确性 |
| 灰度 3 | ACE/iPro/CMS 全应用 | 三应用状态稳定 | 首页健康、告警延迟、通知成功率 |
| 灰度 4 | 20 台服务器 | Agent 心跳稳定 | Agent 无心跳、资源指标缺失 |
| 灰度 5 | Kafka/PostgreSQL | 默认规则无明显误报 | 积压、连接、慢 SQL、容量告警准确性 |

灰度期间默认只启用 P1/P2 核心规则，P3 或高噪声规则先观察不通知。

## 7. 平台自监控

平台自身必须纳入监控，覆盖 API QPS/错误率/P95、数据源查询成功率/超时、告警检测延迟/积压、通知发送成功率、日志查询超时率、业务库连接池/慢 SQL/锁等待、审计写入成功率、Agent 心跳、前端资源加载失败。

## 8. 回滚方案

| 对象 | 回滚方式 |
| --- | --- |
| 应用接入 | 将应用接入状态改为未接入或停用绑定，不删除资产数据 |
| 告警规则 | 停用规则或切回观察模式，保留事件和审计记录 |
| 数据源 | 停用数据源绑定，保留配置草稿和失败原因 |
| Agent | 停止采集或回退到上一版本配置 |
| Kafka/PostgreSQL 模板 | 停用默认规则，保留对象、依赖和指标查询能力 |
| 邮件通知 | 切换为仅记录通知任务，暂停真实发送 |
| 权限配置 | 回退到上一版应用授权和角色配置，记录审计 |
| 脱敏策略 | 回退到上一版策略，禁止回滚为明文默认展示 |
