import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function CsvParserNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="📋"
      colorCls="border-violet-500" selected={selected}>
      <span className="text-gray-400">
        {config.hasHeader === 'false' ? 'No header' : 'With header'} · {config.separator || ','}
      </span>
    </BaseNode>
  )
}
