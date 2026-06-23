import { useMemo, useState } from 'react'
import './styles/app.css'
import {
  collectRecursiveImpact,
  entityTypeMeta,
  groupSearchResults,
  lineageEntities,
  searchLineageEntities,
  traceColumnPath
} from './data/lineageModel'

const pageSystem = [
  '首页：全局搜索 + 热门实体 + 最近访问',
  '血缘图页：Canvas 交互式 DAG 图',
  '实体详情页：Schema / Lineage / Usage / Queries',
  '影响分析页：变更模拟 + 递归影响高亮'
]

const componentStates = [
  { name: '加载中', description: '骨架屏 + Canvas 节点占位，搜索框保持可输入。' },
  { name: '空数据', description: '提示没有匹配实体，并给出改用库名、表名或 Owner 搜索的建议。' },
  { name: '错误', description: '展示可重试按钮，保留用户当前查询条件与已展开层级。' },
  { name: '正常', description: '展示分组结果、迷你血缘预览、右侧滑出详情与影响分析操作。' }
]

const asciiSketch = `
+---------------- SQL 血缘平台首页 ----------------+
| 全局搜索 [ ods_order / 指标 / Owner ]             |
|--------------------------------------------------|
| 类型分组: Dataset | Job | Dashboard | ML Model   |
| 迷你血缘: ods_order -> dwd -> dws -> dashboard   |
|--------------------------------------------------|
| Canvas DAG: 逐层展开节点 + 字段级下钻按钮         |
|                                  +-------------+ |
|                                  | Slide-out   | |
|                                  | Schema Tabs | |
|                                  +-------------+ |
+--------------------------------------------------+
`

const miniPreview = 'ods_order.amount -> dwd_order_detail.pay_amount -> dws_trade_summary.gmv'

function App() {
  const [showColumnPath, setShowColumnPath] = useState(false)
  const [query, setQuery] = useState('')
  const [notifiedOwner, setNotifiedOwner] = useState('')
  const filteredEntities = useMemo(() => searchLineageEntities(query), [query])
  const searchGroups = useMemo(() => groupSearchResults(filteredEntities), [filteredEntities])
  const columnPath = useMemo(() => traceColumnPath('ads_revenue_dashboard', 'gmv'), [])
  const impactItems = useMemo(() => collectRecursiveImpact('ods_order', 'amount'), [])

  return (
    <main className="lineage-shell">
      <section className="hero">
        <div>
          <p className="eyebrow">Data Governance Frontend</p>
          <h1>SQL 血缘平台</h1>
          <p className="hero-copy">
            面向数据开发、分析师与治理 Owner，解决发现表、评估字段变更影响、查询血缘上下游和通知责任人的闭环场景。
          </p>
        </div>
        <label className="search-box">
          <span>全局搜索</span>
          <input
            value={query}
            placeholder="搜索表、任务、看板、模型，例如 ods_order"
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
      </section>

      <section className="page-system">
        {pageSystem.map((item) => (
          <article key={item} className="page-card">
            {item}
          </article>
        ))}
      </section>

      <section className="design-grid">
        <article className="panel">
          <h2>页面布局 ASCII 草图</h2>
          <pre>{asciiSketch}</pre>
        </article>

        <article className="panel">
          <h2>组件交互状态</h2>
          <div className="state-list">
            {componentStates.map((state) => (
              <div className="state-item" key={state.name}>
                <strong>{state.name}</strong>
                <span>{state.description}</span>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="workspace">
        <aside className="panel search-panel" aria-label="搜索结果分组">
          <h2>搜索为中心</h2>
          <p className="muted">结果按实体类型分组，并在列表中展示迷你血缘预览。</p>
          {filteredEntities.length === 0 ? (
            <div className="empty-state">没有匹配实体，请改用库名、表名或 Owner 搜索。</div>
          ) : null}
          {searchGroups.map((group) => (
            <div className="group" key={group.type}>
              <div className="group-title" style={{ color: entityTypeMeta[group.type].color }}>
                {group.type}
                <span>{group.label}</span>
              </div>
              <ul>
                {group.items.map((entity) => (
                  <li key={entity.id}>
                    <b>{entity.name}</b>
                    <small>{entity.owner}</small>
                  </li>
                ))}
              </ul>
            </div>
          ))}
          <div className="mini-preview">{miniPreview}</div>
        </aside>

        <section className="panel canvas-panel">
          <div className="panel-heading">
            <div>
              <h2>血缘图 Canvas</h2>
              <p className="muted">采用逐层展开的 React Flow 节点模型，避免一次性全量渲染。</p>
            </div>
            <button type="button" onClick={() => setShowColumnPath(true)}>
              查看 gmv 字段级血缘
            </button>
          </div>

          <div className="dag">
            {['ods_order', 'dwd_order_detail', 'job_trade_summary', 'dws_trade_summary', 'ads_revenue_dashboard'].map(
              (node, index) => (
                <div className="dag-node" key={node}>
                  <span>{index + 1}</span>
                  {node}
                </div>
              )
            )}
          </div>

          {showColumnPath ? (
            <ol className="column-path" aria-label="列级血缘钻取路径">
              {columnPath.map((step) => (
                <li key={`${step.entityId}.${step.columnName}`}>
                  <strong>
                    {step.entityName}.{step.columnName}
                  </strong>
                  {step.transform ? <small>{step.transform}</small> : null}
                </li>
              ))}
            </ol>
          ) : (
            <p className="hint">钻取路径：表级节点 {'->'} Schema Tab {'->'} 字段 gmv {'->'} 上游字段映射链路。</p>
          )}
        </section>

        <aside className="slideout">
          <h2>右侧滑出面板</h2>
          <div className="tabs">
            <span>Schema</span>
            <span>Lineage</span>
            <span>Usage</span>
            <span>Queries</span>
          </div>
          <p>当前实体：ads_revenue_dashboard</p>
          <p>字段级入口：gmv {'->'} 查看上游转换 SQL 与下游引用。</p>
        </aside>
      </section>

      <section className="panel impact" aria-label="影响分析结果">
        <div>
          <h2>影响分析闭环</h2>
          <p className="muted">模拟变更：ods_order.amount 类型调整，递归高亮下游实体并生成通知 Owner 动作。</p>
        </div>
        <div className="impact-list">
          {impactItems.map((item) => (
            <article className="impact-card" key={`${item.entityId}.${item.columnName}`}>
              <strong>{item.entityName}</strong>
              <span>
                第 {item.depth} 层影响：{item.columnName}
              </span>
              <button type="button" onClick={() => setNotifiedOwner(item.owner)}>
                通知 {item.owner}
              </button>
            </article>
          ))}
        </div>
        {notifiedOwner ? <div className="notice-result">已生成给 {notifiedOwner} 的影响通知</div> : null}
      </section>
    </main>
  )
}

export default App
