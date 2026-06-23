# SQL 血缘平台前端

这是一个独立的 SQL 血缘平台前端原型，聚焦全局搜索、血缘 DAG、列级血缘钻取、影响分析与 Owner 通知闭环。

## 启动

```bash
npm install
npm run dev
```

## 验证

```bash
npm test
npm run build
```

## 功能范围

- 首页：全局搜索、实体类型分组、迷你血缘预览。
- 血缘图页：Canvas 风格 DAG 展示，保留 React Flow 接入依赖。
- 实体详情页：右侧 Slide-out 面板，包含 Schema / Lineage / Usage / Queries。
- 影响分析页：模拟字段变更，递归展示下游实体并生成通知 Owner 操作。

## 列级血缘路径

```text
ads_revenue_dashboard.gmv
  -> dws_trade_summary.gmv
  -> dwd_order_detail.pay_amount
  -> ods_order.amount
```

## 目录结构

```text
src/
  App.tsx                         页面原型入口
  data/lineageModel.ts            血缘实体、字段映射与影响分析模型
  styles/app.css                  页面样式
  __tests__/App.test.tsx          页面行为测试
  __tests__/lineageModel.test.ts  领域模型测试
```
