# 指标字典初版与默认阈值

## 1. 文档说明

本文作为 Sprint 0 第一周交付物，基于以下文档形成一期指标字典初版、默认告警阈值和待业务确认项：

- `demand/monitoring-platform-requirements.md`
- `architecture/technical-architecture-design.md`
- `middleware-db-monitoring/requirements-architecture/middleware-db-requirements-architecture.md`

一期指标遵循“统一监控对象 + 单指标简单阈值 + 应用授权衍生可见”的口径。指标数据优先复用 Prometheus/Grafana、SkyWalking、日志源、Agent、Kafka Exporter/JMX Exporter、PostgreSQL Exporter 等来源；一期正式通知渠道优先使用邮件，短信、电话、IM、Webhook 仅做模型预留。

## 2. 指标编码规范

| 前缀 | 指标域 | 示例 |
| --- | --- | --- |
| `app.` | 应用指标 | `app.health.status` |
| `host.` | 服务器指标 | `host.cpu.usage_pct` |
| `kafka.` | Kafka 指标 | `kafka.consumer.lag` |
| `pg.` | PostgreSQL 指标 | `pg.connection.usage_pct` |
| `log.` | 日志指标 | `log.error.count_5m` |
| `biz.` | 业务指标 | `biz.payment.success_rate` |

## 3. 应用指标字典

| 编码 | 名称 | 对象 | 单位 | 来源 | 采集周期 | 一期用途 |
| --- | --- | --- | --- | --- | --- | --- |
| `app.health.status` | 应用健康状态 | 应用/实例 | 枚举 | 健康检查、Agent、Prometheus Target | 30 秒 | 首页健康、应用详情、健康告警 |
| `app.instance.count` | 在线实例数 | 应用 | 个 | 注册中心、Agent、Prometheus | 30 秒 | 应用列表、容量异常判断 |
| `app.request.qps` | 请求 QPS | 应用/接口 | 次/秒 | SkyWalking、Prometheus、网关指标 | 30 秒 | 应用详情、接口趋势、异常突增/突降观察 |
| `app.error.rate` | 请求错误率 | 应用/接口 | % | SkyWalking、Prometheus、日志聚合 | 30 秒 | 应用告警、TOP 异常应用 |
| `app.response.avg_ms` | 平均响应时间 | 应用/接口 | ms | SkyWalking、Prometheus | 30 秒 | 性能趋势、慢接口定位 |
| `app.response.p95_ms` | P95 响应时间 | 应用/接口 | ms | SkyWalking、Prometheus | 30 秒 | 默认性能告警、慢接口排行 |
| `app.response.p99_ms` | P99 响应时间 | 应用/接口 | ms | SkyWalking、Prometheus | 30 秒 | 极端延迟分析 |
| `app.jvm.heap_usage_pct` | JVM 堆内存使用率 | Java 应用/实例 | % | JMX Exporter、Prometheus | 30 秒 | Java 应用运行时排障 |
| `app.jvm.gc.count_5m` | 5 分钟 GC 次数 | Java 应用/实例 | 次 | JMX Exporter、Prometheus | 1 分钟 | GC 异常观察 |
| `app.thread.count` | 线程数 | 应用/实例 | 个 | JMX Exporter、Agent | 1 分钟 | 线程泄漏和资源异常观察 |

## 4. 服务器指标字典

| 编码 | 名称 | 对象 | 单位 | 来源 | 采集周期 | 一期用途 |
| --- | --- | --- | --- | --- | --- | --- |
| `host.cpu.usage_pct` | CPU 使用率 | 服务器 | % | Node Exporter、Agent、Prometheus | 30 秒 | 服务器列表、资源告警 |
| `host.load.1m` | 1 分钟负载 | 服务器 | load | Node Exporter、Agent | 30 秒 | CPU 压力辅助判断 |
| `host.memory.usage_pct` | 内存使用率 | 服务器 | % | Node Exporter、Agent | 30 秒 | 服务器列表、资源告警 |
| `host.swap.usage_pct` | Swap 使用率 | 服务器 | % | Node Exporter、Agent | 1 分钟 | 内存压力辅助判断 |
| `host.disk.usage_pct` | 磁盘使用率 | 服务器/挂载点 | % | Node Exporter、Agent | 1 分钟 | 磁盘容量告警 |
| `host.disk.io_util_pct` | 磁盘 IO 利用率 | 服务器/磁盘 | % | Node Exporter、Agent | 1 分钟 | IO 瓶颈排障 |
| `host.network.in_bps` | 网络入流量 | 服务器/网卡 | bps | Node Exporter、Agent | 30 秒 | 网络趋势观察 |
| `host.network.out_bps` | 网络出流量 | 服务器/网卡 | bps | Node Exporter、Agent | 30 秒 | 网络趋势观察 |
| `host.network.packet_loss_pct` | 网络丢包率 | 服务器/网卡 | % | Agent、网络探测 | 1 分钟 | 网络异常告警 |
| `host.agent.heartbeat_age_sec` | Agent 心跳延迟 | 服务器/Agent | 秒 | Agent 管理服务 | 30 秒 | 接入状态、采集异常提示 |

## 5. Kafka 指标字典

| 编码 | 名称 | 对象 | 单位 | 来源 | 采集周期 | 一期用途 |
| --- | --- | --- | --- | --- | --- | --- |
| `kafka.broker.up` | Broker 存活 | Kafka Broker | 0/1 | Kafka Exporter、JMX Exporter、Prometheus | 30 秒 | Broker 不可用告警 |
| `kafka.topic.messages_in_rate` | Topic 生产速率 | Topic | 条/秒 | Kafka Exporter、JMX Exporter | 30 秒 | 生产突增/突降观察 |
| `kafka.consumer.records_rate` | 消费速率 | Consumer Group/Topic | 条/秒 | Kafka Exporter、JMX Exporter | 30 秒 | 消费停滞判断 |
| `kafka.consumer.lag` | 消息积压量 | Consumer Group/Topic/Partition | 条 | Kafka Exporter、Prometheus | 30 秒 | MQ 积压告警、影响应用定位 |
| `kafka.consumer.lag_seconds` | 消费延迟 | Consumer Group/Topic | 秒 | Kafka Exporter、业务埋点 | 1 分钟 | 业务延迟告警 |
| `kafka.consumer.error_rate` | 消费失败率 | Consumer Group | % | 应用指标、日志聚合、Prometheus | 1 分钟 | 消费异常排障 |
| `kafka.dead_letter.count_5m` | 5 分钟死信新增数 | Topic/死信队列 | 条 | 应用日志、业务指标、MQ 采集 | 1 分钟 | 死信告警 |
| `kafka.broker.disk_usage_pct` | Broker 磁盘使用率 | Kafka Broker/节点 | % | Node Exporter、JMX Exporter | 1 分钟 | Broker 存储水位告警 |

## 6. PostgreSQL 指标字典

| 编码 | 名称 | 对象 | 单位 | 来源 | 采集周期 | 一期用途 |
| --- | --- | --- | --- | --- | --- | --- |
| `pg.instance.up` | 实例存活 | PostgreSQL 实例 | 0/1 | PostgreSQL Exporter、Prometheus | 30 秒 | 数据库不可用告警 |
| `pg.connection.usage_pct` | 连接使用率 | PostgreSQL 实例/库 | % | PostgreSQL Exporter | 30 秒 | 连接池/连接耗尽告警 |
| `pg.connection.active_count` | 活跃连接数 | PostgreSQL 实例/库 | 个 | PostgreSQL Exporter | 30 秒 | 会话异常观察 |
| `pg.qps` | 查询 QPS | PostgreSQL 实例/库 | 次/秒 | PostgreSQL Exporter、pg_stat | 30 秒 | 数据库吞吐趋势 |
| `pg.tps` | 事务 TPS | PostgreSQL 实例/库 | 次/秒 | PostgreSQL Exporter、pg_stat | 30 秒 | 事务吞吐趋势 |
| `pg.slow_sql.count_5m` | 5 分钟慢 SQL 数量 | PostgreSQL 实例/库 | 条 | pg_stat_statements、日志聚合、Exporter | 1 分钟 | 慢 SQL 告警 |
| `pg.lock_wait.count` | 锁等待数 | PostgreSQL 实例/库 | 个 | PostgreSQL Exporter、pg_stat_activity | 30 秒 | 锁等待/阻塞告警 |
| `pg.deadlock.count_5m` | 5 分钟死锁数 | PostgreSQL 实例/库 | 次 | PostgreSQL Exporter、数据库日志 | 1 分钟 | 死锁告警 |
| `pg.replication.lag_seconds` | 复制延迟 | PostgreSQL 主从/副本 | 秒 | PostgreSQL Exporter | 30 秒 | 主从延迟告警 |
| `pg.storage.usage_pct` | 存储使用率 | PostgreSQL 实例/库/表空间 | % | PostgreSQL Exporter、Node Exporter | 1 分钟 | 容量告警 |
| `pg.cache.hit_rate` | Cache 命中率 | PostgreSQL 实例/库 | % | PostgreSQL Exporter、pg_stat | 1 分钟 | 性能劣化观察 |

## 7. 日志指标字典

| 编码 | 名称 | 对象 | 单位 | 来源 | 采集周期 | 一期用途 |
| --- | --- | --- | --- | --- | --- | --- |
| `log.error.count_5m` | 5 分钟 ERROR 日志数 | 应用/实例 | 条 | ELK、Loki、日志源 API | 1 分钟 | ERROR 日志告警、错误趋势 |
| `log.fatal.count_5m` | 5 分钟 FATAL 日志数 | 应用/实例 | 条 | ELK、Loki、日志源 API | 1 分钟 | 严重错误告警 |
| `log.warn.count_5m` | 5 分钟 WARN 日志数 | 应用/实例 | 条 | ELK、Loki、日志源 API | 1 分钟 | 风险观察 |
| `log.error.signature_count_5m` | 5 分钟错误签名数 | 应用 | 个 | 日志错误聚合服务 | 1 分钟 | 错误聚合、TOP 错误 |
| `log.ingest.delay_seconds` | 日志采集延迟 | 应用/实例/日志源 | 秒 | 日志采集服务、日志源状态 | 1 分钟 | 日志源采集异常提示 |
| `log.query.error_rate` | 日志查询失败率 | 日志源/平台服务 | % | 平台自身监控 | 1 分钟 | 平台自身可观测性 |

## 8. 业务指标字典

| 编码 | 名称 | 对象 | 单位 | 来源 | 采集周期 | 一期用途 |
| --- | --- | --- | --- | --- | --- | --- |
| `biz.order.count_5m` | 5 分钟订单量 | 业务线/应用/场景 | 单 | 业务数据库、业务埋点、指标 API | 1 分钟 | 首页业务卡片、订单趋势 |
| `biz.payment.success_rate` | 支付成功率 | 业务线/支付链路 | % | 支付应用、业务数据库、指标 API | 1 分钟 | 核心业务告警 |
| `biz.purchase.order_count_5m` | 5 分钟采购单量 | 采购业务/应用 | 单 | 采购系统、业务数据库 | 1 分钟 | 采购场景业务监控 |
| `biz.logistics.order_count_5m` | 5 分钟物流单量 | 物流业务/应用 | 单 | 物流系统、业务数据库 | 1 分钟 | 物流场景业务监控 |
| `biz.task.backlog_count` | 任务积压量 | 业务任务/调度任务 | 个 | 调度平台、业务数据库、指标 API | 1 分钟 | 首页业务卡片、积压告警 |
| `biz.task.failure_count_5m` | 5 分钟任务失败数 | 业务任务/调度任务 | 次 | 调度平台、应用日志、业务数据库 | 1 分钟 | 任务失败告警 |
| `biz.sync.delay_seconds` | 数据同步延迟 | 同步任务/数据链路 | 秒 | 同步系统、业务埋点、指标 API | 1 分钟 | 数据延迟告警 |
| `biz.register.count_5m` | 5 分钟注册数 | 用户业务/应用 | 人 | 业务数据库、指标 API | 1 分钟 | 业务趋势观察 |

## 9. 默认告警阈值表

默认阈值用于一期接入时生成规则模板。正式启用前应结合应用等级、环境、业务时段和历史基线调整；未接入指标或数据源异常的对象不可启用依赖该指标的告警规则。

| 对象类型 | 指标编码 | 默认触发条件 | 持续时间 | 检测周期 | 默认级别 | 一期通知对象 | 说明 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 应用 | `app.health.status` | 连续 3 次健康检查失败 | 约 90 秒 | 30 秒 | P1 | 应用负责人、值班组 | 核心生产应用可配置为 P0 |
| 应用 | `app.error.rate` | `> 5%` | 5 分钟 | 1 分钟 | P1 | 应用负责人、值班组 | 低流量应用需设置最小请求量门槛 |
| 应用 | `app.response.p95_ms` | `> 2000 ms` | 5 分钟 | 1 分钟 | P2 | 应用负责人、值班组 | 接口级阈值可按核心接口单独配置 |
| 应用 | `app.jvm.heap_usage_pct` | `> 90%` | 5 分钟 | 1 分钟 | P2 | 应用负责人 | Java 应用默认启用 |
| 服务器 | `host.cpu.usage_pct` | `> 85%` | 5 分钟 | 1 分钟 | P2 | 服务器负责人、运维/SRE 值班组 | 结合负载判断误报 |
| 服务器 | `host.memory.usage_pct` | `> 90%` | 5 分钟 | 1 分钟 | P2 | 服务器负责人、运维/SRE 值班组 | Swap 持续升高时需提升关注 |
| 服务器 | `host.disk.usage_pct` | `> 85%` | 5 分钟 | 1 分钟 | P2 | 服务器负责人、运维/SRE 值班组 | 容量预警 |
| 服务器 | `host.disk.usage_pct` | `> 95%` | 5 分钟 | 1 分钟 | P1 | 服务器负责人、运维/SRE 值班组 | 容量高危 |
| 服务器 | `host.agent.heartbeat_age_sec` | `> 180 秒` | 3 分钟 | 1 分钟 | P2 | 运维/SRE 值班组 | 表示采集风险，不直接等同业务异常 |
| Kafka | `kafka.broker.up` | `= 0` | 连续 3 次 | 30 秒 | P1 | 对象负责人、关联应用负责人、值班组 | 核心链路可配置 P0 |
| Kafka | `kafka.consumer.lag` | `> 业务阈值` | 10 分钟 | 1 分钟 | P2 | 对象负责人、关联应用负责人、值班组 | 默认需按 Topic/消费组确认具体数值 |
| Kafka | `kafka.consumer.records_rate` | 有积压且 `= 0` | 5 分钟 | 1 分钟 | P1 | 对象负责人、关联应用负责人、值班组 | 用于消费停滞 |
| Kafka | `kafka.dead_letter.count_5m` | `> 0` 或 `> 业务阈值` | 5 分钟 | 1 分钟 | P2 | 对象负责人、关联应用负责人 | 需确认死信口径 |
| Kafka | `kafka.broker.disk_usage_pct` | `> 85%` | 5 分钟 | 1 分钟 | P2 | 对象负责人、运维/SRE 值班组 | 存储预警 |
| Kafka | `kafka.broker.disk_usage_pct` | `> 95%` | 5 分钟 | 1 分钟 | P1 | 对象负责人、运维/SRE 值班组 | 存储高危 |
| PostgreSQL | `pg.instance.up` | `= 0` | 连续 3 次 | 30 秒 | P1 | 数据库负责人、关联应用负责人、值班组 | 核心生产库可配置 P0 |
| PostgreSQL | `pg.connection.usage_pct` | `> 80%` | 5 分钟 | 1 分钟 | P2 | 数据库负责人、关联应用负责人 | 连接预警 |
| PostgreSQL | `pg.connection.usage_pct` | `> 90%` | 5 分钟 | 1 分钟 | P1 | 数据库负责人、关联应用负责人、值班组 | 连接高危 |
| PostgreSQL | `pg.slow_sql.count_5m` | `> 业务阈值` | 5 分钟 | 1 分钟 | P2 | 数据库负责人、关联应用负责人 | 默认数值需结合应用确认 |
| PostgreSQL | `pg.lock_wait.count` | `> 0` 且持续 | 5 分钟 | 1 分钟 | P2 | 数据库负责人、关联应用负责人 | 严重阻塞可提升到 P1 |
| PostgreSQL | `pg.deadlock.count_5m` | `> 0` | 5 分钟 | 1 分钟 | P1 | 数据库负责人、关联应用负责人、值班组 | 默认认为死锁需要及时处理 |
| PostgreSQL | `pg.replication.lag_seconds` | `> 60 秒` | 5 分钟 | 1 分钟 | P1 | 数据库负责人、运维/SRE 值班组 | 只读副本/主从场景启用 |
| PostgreSQL | `pg.storage.usage_pct` | `> 85%` | 5 分钟 | 1 分钟 | P2 | 数据库负责人、运维/SRE 值班组 | 容量预警 |
| PostgreSQL | `pg.storage.usage_pct` | `> 95%` | 5 分钟 | 1 分钟 | P1 | 数据库负责人、运维/SRE 值班组 | 容量高危 |
| 日志 | `log.error.count_5m` | `> 业务阈值` | 5 分钟 | 1 分钟 | P2 | 应用负责人、值班组 | 需按应用流量和日志规范确认阈值 |
| 日志 | `log.fatal.count_5m` | `> 0` | 1 分钟 | 1 分钟 | P1 | 应用负责人、值班组 | FATAL 默认高优先级 |
| 日志 | `log.ingest.delay_seconds` | `> 300 秒` | 5 分钟 | 1 分钟 | P2 | 运维/SRE 值班组 | 表示日志接入风险 |
| 业务 | `biz.payment.success_rate` | `< 业务阈值` | 5 分钟 | 1 分钟 | P0/P1 | 业务负责人、应用负责人、值班组 | 核心支付链路建议 P0 |
| 业务 | `biz.task.backlog_count` | `> 业务阈值` | 10 分钟 | 1 分钟 | P2 | 业务负责人、应用负责人、值班组 | 按任务类型确认阈值 |
| 业务 | `biz.sync.delay_seconds` | `> 业务阈值` | 5 分钟 | 1 分钟 | P1/P2 | 业务负责人、应用负责人、值班组 | 数据链路按影响面定级 |
| 业务 | `biz.task.failure_count_5m` | `> 业务阈值` | 5 分钟 | 1 分钟 | P2/P1 | 业务负责人、应用负责人 | 按核心任务提升级别 |

## 10. 待业务确认项

| 编号 | 待确认项 | 影响范围 | 建议责任方 |
| --- | --- | --- | --- |
| C-01 | ACE、iPro、CMS 等一期应用的正式应用编码、负责人、业务线、部署环境和服务器映射 | 应用指标、权限过滤、告警通知 | 应用负责人、平台管理员 |
| C-02 | 一期约 20 台服务器的主机名、IP、环境、部署应用、Agent 安装状态和负责人 | 服务器指标、Agent 告警、资源看板 | 运维/SRE |
| C-03 | Kafka 集群、Topic、Consumer Group 与应用的依赖关系清单 | Kafka 指标权限、影响范围、通知对象 | 中间件负责人、应用负责人 |
| C-04 | Kafka 积压、消费延迟、死信数量的业务阈值 | Kafka 默认规则启用 | 中间件负责人、业务负责人 |
| C-05 | PostgreSQL 实例、库、只读副本与应用的依赖关系清单 | PostgreSQL 权限、影响范围、告警通知 | 数据库负责人、应用负责人 |
| C-06 | PostgreSQL 慢 SQL 口径、慢 SQL 数量阈值、复制延迟容忍度 | 数据库默认规则启用 | 数据库负责人、运维/SRE |
| C-07 | 物流、采购场景首批业务指标口径、计算公式、数据来源和刷新周期 | 业务指标字典、首页业务卡片、业务告警 | 业务负责人、应用负责人 |
| C-08 | 支付成功率等核心业务指标的分级阈值和影响面定义 | P0/P1 业务告警 | 业务负责人、值班负责人 |
| C-09 | ERROR/FATAL 日志告警阈值是否按应用、环境、流量分层配置 | 日志告警噪音控制 | 应用负责人、运维/SRE |
| C-10 | 生产、预发、测试、开发环境是否使用不同默认阈值和通知策略 | 告警规则模板 | 运维/SRE、平台管理员 |
| C-11 | 邮件通知 SMTP/邮件服务配置、发件账号、模板规范和收件人分组 | 一期通知落地 | 平台管理员、运维/SRE |
| C-12 | 数据库 SQL 文本、库表名、Topic/消费组、业务字段的敏感级别与脱敏规则 | 指标标签、日志查询、告警详情、导出 | 安全负责人、数据库/中间件负责人 |
| C-13 | 共享 Kafka/PostgreSQL 对象的主维护团队和兜底值班组 | 共享对象告警通知 | 运维/SRE、中间件/数据库负责人 |
| C-14 | 默认重复通知间隔是否采用 10 分钟，以及 P0/P1 是否需要更短间隔 | 告警防刷屏、通知策略 | 值班负责人、业务负责人 |

## 11. 一期落地建议

1. 先将本指标字典写入 `metric_definition` 和规则模板配置，按对象类型区分是否默认启用。
2. 接入对象若处于未接入、仅日志、仅指标或采集异常状态，应展示接入风险，但不直接计入业务异常。
3. 对于标记为“业务阈值”的规则，默认生成草稿规则，由对象负责人或业务负责人确认后启用。
4. 同一规则、同一对象、同一维度在未恢复前只保留一个活跃告警事件，重复通知间隔默认 10 分钟。
5. P0/P1 规则在一期仍以邮件通知为正式落地渠道，短信、电话、IM、Webhook 字段预留但不作为启用前置条件。
