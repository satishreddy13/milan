import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function FileWriterNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="💾"
      colorCls="border-teal-500" selected={selected}>
      {config.directory && (
        <span className="truncate max-w-[140px] block" title={config.directory}>
          {config.directory}
        </span>
      )}
    </BaseNode>
  )
}
