# 中间件与数据库监控补充总览

本目录用于承载企业级统一监控平台中“中间件与数据库监控”的补充产物。该补充遵循现有一期约束：不新增一级导航，优先通过应用详情依赖、指标中心、告警中心和接入管理承载。

## 1. 产物清单

| 产物 | 文件 | 内容 |
| --- | --- | --- |
| 需求与信息架构补充 | `requirements-architecture/middleware-db-requirements-architecture.md` | 一期范围、对象类型、核心指标、告警规则、权限模型、导航建议、一期/二期边界 |
| 产品原型补充 | `product-prototype/middleware-db-product-prototype.md` | 中间件/数据库列表、详情、指标、告警、应用依赖区和状态验收 |
| UI 高保真说明 | `ui-design/middleware-db-ui-high-fidelity.md` | 页面布局、状态标签、依赖拓扑、指标图表、慢 SQL/队列积压入口、权限和数据源异常 |
| 静态设计稿 | `../design-drafts/middleware-db/index.html` | 中间件与数据库监控静态高保真画板 |

## 2. 一期覆盖对象

- 中间件：Redis、Kafka、RabbitMQ、RocketMQ、Nginx/API 网关、任务调度平台等。
- 数据库：MySQL、PostgreSQL、Oracle 等关系型数据库，可兼容 SQL Server、MongoDB、Elasticsearch、TiDB、ClickHouse 等扩展对象。
- 关联能力：应用依赖、服务器部署、指标趋势、告警事件、日志/慢 SQL/队列积压入口、数据源状态。

## 3. 评审重点

- 未接入、采集异常和业务异常必须分开展示。
- 中间件/数据库异常必须能关联到影响应用、负责人和值班组。
- 一期告警仍采用单指标简单阈值规则。
- 权限仍以应用授权为主，并通过依赖关系衍生中间件/数据库可见性。
- 数据库敏感信息、连接串、账号、慢 SQL 明文等必须脱敏和审计。

