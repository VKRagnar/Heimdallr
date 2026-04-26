# 企业级统一监控平台技术栈梳理与初步选型

## 1. 结论摘要

当前项目已经完成需求、产品原型、技术架构、详细设计和一期实施拆解。此前 `project/` 目录中的 Node.js 内存版 API 只是原型验证，不作为正式工程底座保留。

结合团队偏 Java 的现实情况，一期正式实现建议采用“Java 后端 + React 前端 + 分层观测存储 + 外部数据源适配”的技术路线。后端以 Spring Boot 模块化单体起步，告警检测、通知投递、导出任务等异步负载以独立进程/Worker 运行；后续如果规模和组织边界变清晰，再拆为微服务。

Java 25 用于开发是可行的：Java 25 已于 2025-09-16 GA，属于 LTS；Spring Boot 4.0.x 官方系统要求支持 Java 17 到 Java 26。因此正式选型建议以 Java 25 + Spring Boot 4 为主线，同时在技术架构上兼容 Go、Node.js、Python 等其它后端技术栈作为边缘适配器、采集器或工具服务。

## 2. 技术栈总览

| 层级 | 初步选型 | 备选/兼容 | 结论 |
| --- | --- | --- | --- |
| 前端框架 | React 19 + TypeScript | Vue 3 | 选 React，复杂后台组件生态、招聘和长期维护更稳 |
| 构建工具 | Vite | Rsbuild、Webpack | 选 Vite，启动快、配置轻，适合中后台 SPA |
| UI 组件 | Ant Design 5 或 6 | Arco Design、Semi Design | 初选 Ant Design，企业后台组件完备；启动阶段锁定具体大版本 |
| 图表 | Apache ECharts 6 | AntV G2、uPlot | 选 ECharts，指标趋势、告警点、资源图、关系图扩展覆盖面足 |
| 前端数据请求 | TanStack Query v5 | SWR、Redux Toolkit Query | 选 TanStack Query，适合服务端状态、轮询和缓存失效 |
| 前端路由 | React Router | TanStack Router | 选 React Router，学习成本低，后台路由场景足够 |
| 后端主语言 | Java 25 LTS | Java 21 LTS | 团队偏 Java，选 Java 25；生产环境需同步确认 JDK 发行版和运维标准 |
| 后端框架 | Spring Boot 4 + Spring Framework 7 | Spring Boot 3.5 + Java 21 | Java 25 主线建议选 Spring Boot 4；若公司标准仍停在 Boot 3，则回退 Java 21 |
| API 风格 | REST + OpenAPI | gRPC、GraphQL | 一期选 REST，便于权限过滤、审计、联调和导出任务建模 |
| 数据访问 | Spring Data JDBC/MyBatis + jOOQ 可选 | Spring Data JPA/Hibernate | 初选 SQL 可控路线；复杂查询、分区和权限过滤更透明 |
| 业务数据库 | PostgreSQL 16/17 | MySQL 8 | 选 PostgreSQL，契合现有 SQL、`uuid`、`jsonb`、分区和审计建模 |
| 缓存/锁 | Redis 7 | KeyDB、Dragonfly | 选 Redis，用于短 TTL 权限缓存、检测锁、任务去重 |
| 异步任务 | Spring Scheduler + ShedLock/Redis Lock 起步，Spring Batch 处理导出 | Quartz、RabbitMQ、Kafka | 一期先轻量；关键事件落 PostgreSQL outbox，后续可接消息队列 |
| 指标存储 | Prometheus，后续 VictoriaMetrics/Thanos | InfluxDB | 一期对接 Prometheus；容量或长期保留上来后再引入 VictoriaMetrics/Thanos |
| 日志检索 | 兼容现有 ELK | OpenSearch、Loki | 既有 ELK 为前提，平台通过日志适配层接 Elasticsearch/Kibana 体系 |
| 链路/APM | SkyWalking 兼容接入 + OpenTelemetry 埋点标准 | Jaeger | 一期查询/跳转 SkyWalking，平台自身和新服务按 OTel 标准埋点 |
| 通知 | SMTP 邮件 | 企业微信/钉钉/Webhook/短信/电话 | 一期邮件正式落地，其他渠道只做模型和接口预留 |
| 鉴权 | 内部 Token/JWT 过渡，预留 SSO/OIDC | Session | 一期可用 Token 启动，接口模型预留 OIDC/LDAP/SSO |
| 测试 | JUnit 5 + AssertJ + Testcontainers + Playwright | Spock、Cypress | 后端集成测试用 Testcontainers，前端端到端用 Playwright |
| 构建 | Gradle Kotlin DSL | Maven | 初选 Gradle；若团队 Maven 规范成熟，可用 Maven |
| 部署 | Docker Compose 起步，Kubernetes 预留 | 物理机 systemd | 一期可 Compose 或 K8s 部署；生产建议容器化并留 K8s 清单 |
| 可观测性 | OpenTelemetry + Micrometer + Prometheus metrics + structured logs | 仅日志 | 平台自身必须从第一阶段接入指标、日志和追踪 |

## 3. 后端选型说明

### 3.1 推荐路线

后端建议采用 Spring Boot 4 + Java 25 的模块化单体，模块边界与架构文档保持一致：

1. `access`：当前用户、角色、菜单、应用授权、业务线和环境范围。
2. `assets`：应用、服务器、实例、负责人和部署关系。
3. `monitor-objects`：Kafka、PostgreSQL、Agent、数据源和依赖关系。
4. `metrics`：指标定义、指标映射、Prometheus 查询适配。
5. `logs`：ELK 日志查询、上下文、错误聚合、脱敏和导出入口。
6. `alerts`：规则、检测、事件状态机、处理记录。
7. `notifications`：邮件渠道、模板、通知记录和值班组。
8. `security`：脱敏、敏感明文授权、导出控制和审计。
9. `integrations`：SkyWalking、Prometheus、Elasticsearch、Agent、Webhook 预留。

Spring Boot 的 Filter/Interceptor、AOP、Bean Validation、Actuator、Micrometer、Security 扩展点适合本项目的统一鉴权、统一响应、审计拦截、数据范围过滤、数据源适配和平台自身可观测性诉求。

### 3.2 Java 25 可行性

Java 25 作为主开发版本是可行的，但要分成“语言版本可行”和“组织落地可行”两层看：

| 维度 | 判断 |
| --- | --- |
| JDK 生命周期 | Java 25 是 LTS，适合新项目作为长期版本 |
| Spring 兼容 | Spring Boot 4.0.x 支持 Java 17 到 Java 26，覆盖 Java 25 |
| 生态兼容 | 主流 Java 生态会逐步跟进，但部分公司内部 SDK、Agent、APM 插件可能滞后 |
| 生产运维 | 需确认镜像基线、CI、扫描工具、APM Agent、代码质量平台是否支持 Java 25 |
| 回退策略 | 如内部平台不支持 Java 25，可用 Java 21 + Spring Boot 3.5 作为保守方案 |

建议技术决策写成：默认 Java 25 + Spring Boot 4；若公司基础设施或内部 SDK 未完成适配，则阶段性降级到 Java 21 + Spring Boot 3.5，业务架构和模块划分不变。

### 3.3 兼顾其它后端技术栈

主平台后端应统一在 Java/Spring Boot，避免权限、审计、告警状态机和导出控制分散。但可以在边缘能力上兼容其它技术栈：

| 技术栈 | 适用边界 |
| --- | --- |
| Go | 高并发采集器、Agent、轻量探针、Prometheus exporter |
| Node.js/TypeScript | 前端工具链、BFF 原型、低风险内部工具；不作为主后端 |
| Python | 离线分析、规则实验、报表脚本、数据清洗任务 |
| Shell/Ansible | 服务器接入、Agent 安装和运维自动化 |

兼容原则是：跨语言服务必须通过 REST/OpenAPI、Prometheus 指标、OpenTelemetry、事件 outbox 或明确消息协议集成，不能直接绕过主平台权限、审计和脱敏链路。

### 3.4 数据访问

业务库继续围绕 PostgreSQL。正式工程建议：

1. 使用 Flyway 或 Liquibase 管理数据库迁移，迁移批次沿用现有 M01-M08 拆分。
2. 以 Spring Data JDBC 或 MyBatis 承载常规 CRUD，复杂查询可引入 jOOQ 或手写 SQL。
3. 告警列表、审计查询、聚合统计、权限范围过滤等场景优先保持 SQL 显式可读。
4. `alert_event`、`notification_record`、`audit_event`、`export_task` 等大表按月分区，应用层封装查询边界。

不建议首选 JPA/Hibernate 作为核心数据访问方案。本项目关系并不只是对象导航，更多是多维权限过滤、审计、分区、聚合和跨对象查询，SQL 可控性更重要。

## 4. 前端选型说明

前端建议采用 React + TypeScript + Vite + Ant Design + TanStack Query + ECharts。

本项目页面是典型企业内部运营平台：大量表格、筛选、抽屉、表单、详情页、状态标签、权限按钮、后台任务、自动刷新和图表。Ant Design 的后台组件覆盖面与 React 生态更契合现有 UI 设计稿；TanStack Query 适合处理告警轮询、通知记录、接入状态、指标查询等服务端状态；ECharts 能覆盖趋势图、阈值线、堆叠图、资源图和后续拓扑/关系图扩展。

前端状态建议分层：

| 状态类型 | 技术 |
| --- | --- |
| 服务端数据、列表、详情、轮询 | TanStack Query |
| URL 筛选、来源页、时间范围 | React Router search params |
| 轻量 UI 状态，如侧边栏、主题、全局环境 | Zustand 或 React Context |
| 表单状态 | Ant Design Form |
| 图表局部交互 | 组件内状态 |

不建议一开始引入 Redux。这个项目的核心复杂度在服务端数据权限、轮询、缓存和查询条件，而不是大型客户端状态机。

## 5. 监控数据与集成选型

### 5.1 指标

一期指标查询以 Prometheus 为主，平台保存指标定义、指标映射和查询模板，不保存指标明细。后续如出现长期保留、跨集群查询或高基数压力，再引入 VictoriaMetrics 或 Thanos。

### 5.2 日志

日志检索以兼容现有 ELK 为前提。平台不新建日志存储选型争议，而是提供 `logs` 适配层对接 Elasticsearch 查询 API，并在平台内统一做权限过滤、脱敏、导出控制和审计。

| 能力 | ELK 兼容策略 |
| --- | --- |
| 索引配置 | 平台保存 `app_id`、`env`、`index_pattern`、时间字段、级别字段和 Trace 字段映射 |
| 关键字检索 | 通过 Elasticsearch Query DSL 生成受控查询 |
| Trace ID 查询 | 标准化 `traceId`、`requestId` 字段映射，不要求所有应用一次性统一字段名 |
| 上下文日志 | 通过时间窗口、实例、Trace ID 或日志游标查询前后文 |
| 错误聚合 | 一期可基于查询聚合和平台侧摘要算法，后续再沉淀错误签名 |
| 脱敏 | 查询结果进入平台后统一脱敏；明文查看必须授权并审计 |
| 导出 | 平台统一异步导出，不能让前端直连 Kibana/Elasticsearch 导出 |

OpenSearch 和 Loki 只作为未来兼容源，不作为一期主选型。

### 5.3 链路与平台自身观测

业务侧已有少量 SkyWalking 时，一期不要重建完整 APM。平台只需要做 Service/Instance 映射、Trace ID 跳转或摘要查询。新开发的平台自身服务从第一天接入 OpenTelemetry，统一输出 traces、metrics、logs，并通过 Micrometer 暴露 Prometheus 指标。

## 6. 告警、任务与通知

告警检测、通知投递、导出任务不建议放在 HTTP 请求链路里同步执行。建议：

1. `monitor-api`：HTTP API、权限、资产、规则、告警处理和审计查询。
2. `alert-worker`：周期性读取启用规则，查询 Prometheus/日志聚合，写入告警事件。
3. `notification-worker`：消费通知任务，发送邮件，写通知记录和失败原因。
4. `export-worker`：异步生成导出文件，执行脱敏、审计和过期清理。
5. `external-event-worker`：处理外部事件 outbox，二期对接工单/IM/Webhook。

一期可以在同一个 Gradle 多模块工程内以不同 Spring Boot 启动入口运行这些进程，部署上先保持一个代码仓库、多个进程入口。

## 7. 工程结构建议

当前 `project/` 原型目录可以删除。正式工程建议直接在仓库根目录建设 Java 后端和前端目录：

```text
data-monitor/
  backend/
    settings.gradle.kts
    build.gradle.kts
    monitor-api/
    alert-worker/
    notification-worker/
    export-worker/
    common-domain/
    common-security/
    common-observability/
    integrations/
    db-migration/
  frontend/
    package.json
    src/
  deploy/
    docker-compose.yml
    k8s/
  docs/
```

如果团队更习惯 Maven，也可以用 Maven multi-module 替代 Gradle。关键不是构建工具本身，而是把 API、Worker、领域模型、安全权限、观测封装和外部适配器分清楚。

## 8. 版本建议

| 技术 | 建议版本策略 |
| --- | --- |
| Java | 25 LTS；基础设施不支持时回退 Java 21 LTS |
| Spring Boot | 4.0.x；回退方案为 3.5.x |
| Spring Framework | 7.x；回退方案为 6.x |
| PostgreSQL | 16 或 17；如生产环境已有标准版本，优先服从公司 DBA 规范 |
| Redis | 7.x |
| Elasticsearch/ELK | 兼容公司现有版本，先通过适配层屏蔽差异 |
| Prometheus | 对接公司现有版本，平台不强行托管 Prometheus |
| React | 19.x |
| Ant Design | 启动开发时锁定 5.x 或 6.x；若选择 6.x，需同步确认 React 版本兼容和主题迁移成本 |
| Node.js | 24 LTS，仅用于前端工具链和少量开发工具 |

## 9. 暂缓决策

以下内容先保留扩展点，不在一期作为硬选型：

1. 完整微服务拆分和服务网格。
2. Kafka/RabbitMQ 作为正式业务消息总线。
3. ClickHouse 作为日志或指标分析库。
4. 完整 OIDC/LDAP/SSO 落地。
5. 完整 CMDB 同步。
6. 工单、事件管理、IM 机器人正式集成。
7. 复杂告警降噪、静默、升级和排班日历。

## 10. 当前代码处理方向

当前 Node.js 原型目录不再保留。后续改造建议：

1. 删除 `project/` 原型目录。
2. 在仓库根目录创建 `backend/` 和 `frontend/`。
3. 后端先搭建 Spring Boot 4 + Java 25 + PostgreSQL + Redis + OpenAPI + Flyway/Liquibase 的正式骨架。
4. 迁移现有接口语义：`/api/v1/me`、应用资产、服务器、实例、应用授权、审计事件。
5. 把权限校验落到 Spring Security/Interceptor + 数据范围服务 + SQL filter。
6. 再逐步补齐 ELK 日志、Prometheus 指标、告警、通知、导出和审计。

## 11. 参考来源

1. Java 25 官方发布时间与 LTS 支持：Oracle Java SE Support Roadmap 与 JDK 25 发布信息。参考：https://www.oracle.com/java/technologies/java-se-support-roadmap.html 与 https://openjdk.org/projects/jdk/25/
2. Spring Boot 官方系统要求：Spring Boot 4.0.x requires Java 17 and is compatible up to and including Java 26。参考：https://docs.spring.io/spring-boot/system-requirements.html
3. React 官方版本页显示最新主版本为 React 19.2。参考：https://react.dev/versions
4. Ant Design 官方介绍其为面向企业级 Web 应用的 React UI 组件库，支持 TypeScript、国际化和主题定制。参考：https://ant.design/docs/react/introduce/
5. TanStack Query 官方文档定位为服务端状态、数据获取、缓存和同步工具。参考：https://tanstack.com/query/
6. Apache ECharts 官方说明其支持丰富图表类型、Canvas/SVG 渲染和大数据渲染。参考：https://echarts.apache.org/
7. Prometheus 官方说明其用于指标采集、时序查询、告警和可视化。参考：https://prometheus.io/
8. OpenTelemetry 官方说明其是厂商中立的观测框架，用于生成、采集和导出 traces、metrics、logs。参考：https://opentelemetry.io/docs/
