import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function HttpRequestNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="📤"
      colorCls="border-orange-500" selected={selected}>
      {config.url && (
        <span className="truncate block max-w-[140px]">{config.method} {config.url}</span>
      )}
    </BaseNode>
  )
}
