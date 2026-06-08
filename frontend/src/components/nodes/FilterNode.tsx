import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function FilterNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="🔽"
      colorCls="border-cyan-500" selected={selected}>
      {config.condition && (
        <code className="truncate max-w-[140px] block text-cyan-700 font-mono"
              title={config.condition}>
          {config.condition}
        </code>
      )}
    </BaseNode>
  )
}
