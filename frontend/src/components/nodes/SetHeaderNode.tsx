import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function SetHeaderNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="🏷"
      colorCls="border-indigo-400" selected={selected}>
      {config.name && (
        <span className="truncate max-w-[140px] block" title={config.expression}>
          <span className="font-medium text-indigo-700">{config.name}</span>
          {config.expression && (
            <span className="text-gray-400"> = {config.expression}</span>
          )}
        </span>
      )}
    </BaseNode>
  )
}
