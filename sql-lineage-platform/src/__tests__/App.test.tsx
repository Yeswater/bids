import { fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from '../App'

describe('SQL 血缘平台前端页面', () => {
  it('展示搜索中心、四类页面体系、ASCII 草图与组件状态说明', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'SQL 血缘平台' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('搜索表、任务、看板、模型，例如 ods_order')).toBeInTheDocument()
    expect(screen.getByText('首页：全局搜索 + 热门实体 + 最近访问')).toBeInTheDocument()
    expect(screen.getByText('血缘图页：Canvas 交互式 DAG 图')).toBeInTheDocument()
    expect(screen.getByText('实体详情页：Schema / Lineage / Usage / Queries')).toBeInTheDocument()
    expect(screen.getByText('影响分析页：变更模拟 + 递归影响高亮')).toBeInTheDocument()
    expect(screen.getByText(/\+---------------- SQL 血缘平台首页 ----------------\+/)).toBeInTheDocument()
    expect(screen.getByText('加载中')).toBeInTheDocument()
    expect(screen.getByText('空数据')).toBeInTheDocument()
    expect(screen.getByText('错误')).toBeInTheDocument()
    expect(screen.getByText('正常')).toBeInTheDocument()
  })

  it('按实体类型分组展示搜索结果并给出迷你血缘预览', () => {
    render(<App />)

    const searchPanel = screen.getByLabelText('搜索结果分组')
    expect(within(searchPanel).getByText('Dataset')).toBeInTheDocument()
    expect(within(searchPanel).getByText('Job')).toBeInTheDocument()
    expect(within(searchPanel).getByText('Dashboard')).toBeInTheDocument()
    expect(within(searchPanel).getByText('ML Model')).toBeInTheDocument()
    expect(screen.getByText('ods_order.amount -> dwd_order_detail.pay_amount -> dws_trade_summary.gmv')).toBeInTheDocument()
  })

  it('输入搜索词后按实体名称、Owner、描述和字段过滤，并在无匹配时展示空态', () => {
    render(<App />)

    const input = screen.getByPlaceholderText('搜索表、任务、看板、模型，例如 ods_order')
    fireEvent.change(input, { target: { value: '算法 Owner' } })

    const searchPanel = screen.getByLabelText('搜索结果分组')
    expect(within(searchPanel).getByText('risk_score_model')).toBeInTheDocument()
    expect(within(searchPanel).queryByText('ods_order')).not.toBeInTheDocument()

    fireEvent.change(input, { target: { value: '不存在的表' } })
    expect(within(searchPanel).getByText('没有匹配实体，请改用库名、表名或 Owner 搜索。')).toBeInTheDocument()
  })

  it('点击字段级血缘后展示从看板指标到源表字段的钻取路径', async () => {
    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: '查看 gmv 字段级血缘' }))

    const drillPath = screen.getByLabelText('列级血缘钻取路径')
    expect(within(drillPath).getByText('ads_revenue_dashboard.gmv')).toBeInTheDocument()
    expect(within(drillPath).getByText('dws_trade_summary.gmv')).toBeInTheDocument()
    expect(within(drillPath).getByText('dwd_order_detail.pay_amount')).toBeInTheDocument()
    expect(within(drillPath).getByText('ods_order.amount')).toBeInTheDocument()
  })

  it('影响分析展示递归影响与 Owner 通知闭环', () => {
    render(<App />)

    const impactPanel = screen.getByLabelText('影响分析结果')
    expect(within(impactPanel).getByText('job_trade_summary')).toBeInTheDocument()
    expect(within(impactPanel).getByText('ads_revenue_dashboard')).toBeInTheDocument()
    expect(within(impactPanel).getByText('risk_score_model')).toBeInTheDocument()
    expect(within(impactPanel).getByText('通知 Data Engineer')).toBeInTheDocument()
    expect(within(impactPanel).getByText('通知 BI Owner')).toBeInTheDocument()
    expect(within(impactPanel).getByText('通知 算法 Owner')).toBeInTheDocument()

    fireEvent.click(within(impactPanel).getByText('通知 BI Owner'))
    expect(within(impactPanel).getByText('已生成给 BI Owner 的影响通知')).toBeInTheDocument()
  })
})
