import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

const strategyLabel: Record<string, string> = {
  line:       'By line',
  csv:        'CSV rows',
  jsonArray:  'JSON array',
  expression: 'Expression',
}

export function SplitterNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  const strategy = config.strategy ?? 'line'
  return (
    <BaseNode label={data.label as string} icon="✂"
      colorCls="border-orange-500" selected={selected}>
      <span className="text-orange-600 font-medium">{strategyLabel[strategy] ?? strategy}</span>
      {config.streaming === 'true' && (
        <span className="ml-1 text-gray-400">· streaming</span>
      )}
    </BaseNode>
  )
}
