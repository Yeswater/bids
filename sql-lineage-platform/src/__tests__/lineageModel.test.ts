import { describe, expect, it } from 'vitest'
import {
  collectRecursiveImpact,
  groupSearchResults,
  lineageEntities,
  traceColumnPath
} from '../data/lineageModel'

describe('SQL 血缘领域模型', () => {
  it('按 Dataset、Job、Dashboard、ML Model 分组搜索结果', () => {
    const grouped = groupSearchResults(lineageEntities)

    expect(grouped.map((group) => group.type)).toEqual(['Dataset', 'Job', 'Dashboard', 'ML Model'])
    expect(grouped.find((group) => group.type === 'Dataset')?.items.map((item) => item.name)).toContain('dwd_order_detail')
  })

  it('从目标指标字段追踪到源表字段，形成列级血缘钻取路径', () => {
    const path = traceColumnPath('ads_revenue_dashboard', 'gmv')

    expect(path.map((step) => `${step.entityName}.${step.columnName}`)).toEqual([
      'ads_revenue_dashboard.gmv',
      'dws_trade_summary.gmv',
      'dwd_order_detail.pay_amount',
      'ods_order.amount'
    ])
  })

  it('递归收集字段变更会影响的下游实体并包含 Owner 通知对象', () => {
    const impact = collectRecursiveImpact('ods_order', 'amount')

    expect(impact.map((item) => item.entityName)).toEqual([
      'dwd_order_detail',
      'job_trade_summary',
      'dws_trade_summary',
      'ads_revenue_dashboard',
      'risk_score_model'
    ])
    expect(impact.find((item) => item.entityName === 'job_trade_summary')?.owner).toBe('Data Engineer')
    expect(impact.find((item) => item.entityName === 'ads_revenue_dashboard')?.owner).toBe('BI Owner')
  })
})
