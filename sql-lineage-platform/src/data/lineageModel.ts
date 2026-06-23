export type EntityType = 'Dataset' | 'Job' | 'Dashboard' | 'ML Model'

export type LineageEntity = {
  id: string
  name: string
  type: EntityType
  owner: string
  description: string
  columns: string[]
}

export type ColumnMapping = {
  fromEntityId: string
  fromColumn: string
  toEntityId: string
  toColumn: string
  transform: string
}

export type ProcessDependency = {
  fromEntityId: string
  fromColumn: string
  processEntityId: string
  processColumn: string
}

export type SearchGroup = {
  type: EntityType
  label: string
  color: string
  items: LineageEntity[]
}

export type ColumnPathStep = {
  entityId: string
  entityName: string
  columnName: string
  type: EntityType
  transform?: string
}

export type ImpactItem = {
  entityId: string
  entityName: string
  type: EntityType
  owner: string
  columnName: string
  depth: number
}

export const entityTypeMeta: Record<EntityType, { label: string; color: string }> = {
  Dataset: { label: '数据集', color: '#2563eb' },
  Job: { label: '加工任务', color: '#7c3aed' },
  Dashboard: { label: '看板', color: '#ea580c' },
  'ML Model': { label: '机器学习模型', color: '#059669' }
}

export const lineageEntities: LineageEntity[] = [
  {
    id: 'ods_order',
    name: 'ods_order',
    type: 'Dataset',
    owner: 'ODS Owner',
    description: '订单原始明细表，承接业务库订单事实。',
    columns: ['order_id', 'user_id', 'amount', 'pay_time']
  },
  {
    id: 'dwd_order_detail',
    name: 'dwd_order_detail',
    type: 'Dataset',
    owner: 'DWD Owner',
    description: '标准化订单明细表，统一金额、用户与支付时间口径。',
    columns: ['order_id', 'user_id', 'pay_amount', 'pay_date']
  },
  {
    id: 'job_trade_summary',
    name: 'job_trade_summary',
    type: 'Job',
    owner: 'Data Engineer',
    description: '每日交易汇总任务，产出主题层交易指标。',
    columns: ['schedule_time', 'sql_text', 'status']
  },
  {
    id: 'dws_trade_summary',
    name: 'dws_trade_summary',
    type: 'Dataset',
    owner: 'DWS Owner',
    description: '交易主题汇总表，沉淀 GMV、订单数、用户数等复用指标。',
    columns: ['biz_date', 'gmv', 'order_count', 'buyer_count']
  },
  {
    id: 'ads_revenue_dashboard',
    name: 'ads_revenue_dashboard',
    type: 'Dashboard',
    owner: 'BI Owner',
    description: '收入经营看板，面向管理层展示 GMV 趋势与核心转化。',
    columns: ['biz_date', 'gmv', 'order_count']
  },
  {
    id: 'risk_score_model',
    name: 'risk_score_model',
    type: 'ML Model',
    owner: '算法 Owner',
    description: '风控评分模型，使用交易金额与订单频次识别异常账户。',
    columns: ['user_id', 'amount_feature', 'order_frequency', 'risk_score']
  }
]

export const columnMappings: ColumnMapping[] = [
  {
    fromEntityId: 'ods_order',
    fromColumn: 'amount',
    toEntityId: 'dwd_order_detail',
    toColumn: 'pay_amount',
    transform: 'cast(amount as decimal(18, 2))'
  },
  {
    fromEntityId: 'dwd_order_detail',
    fromColumn: 'pay_amount',
    toEntityId: 'dws_trade_summary',
    toColumn: 'gmv',
    transform: 'sum(pay_amount)'
  },
  {
    fromEntityId: 'dws_trade_summary',
    fromColumn: 'gmv',
    toEntityId: 'ads_revenue_dashboard',
    toColumn: 'gmv',
    transform: 'select gmv'
  },
  {
    fromEntityId: 'dwd_order_detail',
    fromColumn: 'pay_amount',
    toEntityId: 'risk_score_model',
    toColumn: 'amount_feature',
    transform: 'feature_normalize(pay_amount)'
  }
]

export const processDependencies: ProcessDependency[] = [
  {
    fromEntityId: 'dwd_order_detail',
    fromColumn: 'pay_amount',
    processEntityId: 'job_trade_summary',
    processColumn: 'sql_text'
  }
]

const entityOrder: EntityType[] = ['Dataset', 'Job', 'Dashboard', 'ML Model']

export function findEntity(entityId: string) {
  return lineageEntities.find((entity) => entity.id === entityId)
}

export function groupSearchResults(results: LineageEntity[]): SearchGroup[] {
  return entityOrder.map((type) => ({
    type,
    label: entityTypeMeta[type].label,
    color: entityTypeMeta[type].color,
    items: results.filter((item) => item.type === type)
  }))
}

export function searchLineageEntities(query: string, entities: LineageEntity[] = lineageEntities): LineageEntity[] {
  const normalizedQuery = query.trim().toLowerCase()
  if (!normalizedQuery) return entities

  return entities.filter((entity) => {
    const searchableText = [entity.name, entity.type, entity.owner, entity.description, ...entity.columns]
      .join(' ')
      .toLowerCase()
    return searchableText.includes(normalizedQuery)
  })
}

export function traceColumnPath(entityId: string, columnName: string): ColumnPathStep[] {
  const entity = findEntity(entityId)
  if (!entity) return []

  const upstreamMapping = columnMappings.find(
    (mapping) => mapping.toEntityId === entityId && mapping.toColumn === columnName
  )

  const current: ColumnPathStep = {
    entityId: entity.id,
    entityName: entity.name,
    columnName,
    type: entity.type,
    transform: upstreamMapping?.transform
  }

  if (!upstreamMapping) {
    return [current]
  }

  return [current, ...traceColumnPath(upstreamMapping.fromEntityId, upstreamMapping.fromColumn)]
}

export function collectRecursiveImpact(entityId: string, columnName: string): ImpactItem[] {
  const visited = new Set<string>()
  const impact: ImpactItem[] = []

  function walk(currentEntityId: string, currentColumnName: string, depth: number) {
    const processImpacts = processDependencies.filter(
      (dependency) => dependency.fromEntityId === currentEntityId && dependency.fromColumn === currentColumnName
    )
    for (const dependency of processImpacts) {
      const entity = findEntity(dependency.processEntityId)
      if (!entity) continue

      const visitKey = `${dependency.processEntityId}.${dependency.processColumn}`
      if (visited.has(visitKey)) continue
      visited.add(visitKey)

      impact.push({
        entityId: entity.id,
        entityName: entity.name,
        type: entity.type,
        owner: entity.owner,
        columnName: dependency.processColumn,
        depth
      })
    }

    const downstreamMappings = columnMappings.filter(
      (mapping) => mapping.fromEntityId === currentEntityId && mapping.fromColumn === currentColumnName
    )

    for (const mapping of downstreamMappings) {
      const entity = findEntity(mapping.toEntityId)
      if (!entity) continue

      const visitKey = `${mapping.toEntityId}.${mapping.toColumn}`
      if (visited.has(visitKey)) continue
      visited.add(visitKey)

      impact.push({
        entityId: entity.id,
        entityName: entity.name,
        type: entity.type,
        owner: entity.owner,
        columnName: mapping.toColumn,
        depth
      })
      walk(mapping.toEntityId, mapping.toColumn, depth + 1)
    }
  }

  walk(entityId, columnName, 1)
  return impact
}
