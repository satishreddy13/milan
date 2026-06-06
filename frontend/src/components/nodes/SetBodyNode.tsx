import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function SetBodyNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="✏️"
      colorCls="border-purple-500" selected={selected}>
      {config.expression && (
        <span className="truncate block max-w-[140px] font-mono">{config.expression}</span>
      )}
    </BaseNode>
  )
}
