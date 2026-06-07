import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function FileReaderNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="📂"
      colorCls="border-amber-500" isSource selected={selected}>
      {config.directory && (
        <span className="truncate max-w-[140px] block" title={config.directory}>
          {config.directory}
        </span>
      )}
      {config.parser && config.parser !== 'none' && (
        <span className="text-violet-500 font-medium">{config.parser.toUpperCase()}</span>
      )}
    </BaseNode>
  )
}
