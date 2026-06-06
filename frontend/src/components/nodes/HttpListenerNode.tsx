import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function HttpListenerNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="🌐"
      colorCls="border-green-500" isSource selected={selected}>
      {config.method && config.path && (
        <span>{config.method} {config.path}</span>
      )}
    </BaseNode>
  )
}
