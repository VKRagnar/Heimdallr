# 企业级统一监控平台设计稿总览

本目录存放企业级统一监控平台一期静态高保真设计稿。所有页面均为本地 HTML/CSS，可直接在浏览器中打开评审，不依赖外部 CDN 或网络资源。

## 1. 设计稿入口

| 类别 | 打开文件 | 覆盖范围 |
| --- | --- | --- |
| 核心排障链路 | `core-pages/index.html` | 排障视角首页、告警详情、日志查询、应用详情、指标趋势 |
| 配置管理链路 | `config-pages/index.html` | 告警规则、规则表单抽屉、值班组/联系人、通知渠道、用户权限/应用授权、应用接入/数据源管理 |
| 列表查询与状态页 | `list-pages/index.html` | 应用列表、服务器列表、告警列表、指标查询、通知记录、日志查询补充态、通用状态页、导出任务状态 |
| 中间件与数据库监控 | `middleware-db/index.html` | 中间件列表、数据库列表、中间件详情、数据库详情、应用依赖摘要/拓扑、告警与指标联动 |
| 评审索引 | `review-index/review-index.md` | 评审顺序、验收清单、冻结条件 |

## 2. 评审建议

1. 先打开 `review-index/review-index.md` 确认评审口径。
2. 再评审 `core-pages/index.html`，走通核心排障链路。
3. 然后评审 `list-pages/index.html`，确认列表、查询、状态和导出任务。
4. 接着评审 `middleware-db/index.html`，确认中间件、数据库和应用依赖的监控视图。
5. 最后评审 `config-pages/index.html`，确认配置、权限、接入和审计。

## 3. 设计依据

- `ui-design/foundation-rules/foundation-rules.md`
- `ui-design/visual-system/visual-system.md`
- `ui-design/core-pages/core-pages-high-fidelity.md`
- `ui-design/list-pages/list-pages-high-fidelity.md`
- `ui-design/config-pages/config-pages-high-fidelity.md`
- `ui-design/components-and-review/components-and-review.md`
- `middleware-db-monitoring/requirements-architecture/middleware-db-requirements-architecture.md`
- `middleware-db-monitoring/product-prototype/middleware-db-product-prototype.md`
- `middleware-db-monitoring/ui-design/middleware-db-ui-high-fidelity.md`
