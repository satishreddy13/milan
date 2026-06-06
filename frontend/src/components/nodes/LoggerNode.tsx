import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function LoggerNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="📋"
      colorCls="border-blue-500" selected={selected}>
      {config.level && <span>[{config.level}]</span>}
    </BaseNode>
  )
}
