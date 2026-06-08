import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function WireTapNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="📡"
      colorCls="border-sky-500" selected={selected}>
      <span className="font-medium text-sky-600">
        {config.destination === 'file' ? '→ file' : '→ log'}
      </span>
      {config.destination === 'file' && config.directory && (
        <span className="block truncate text-gray-400 max-w-[140px]"
              title={config.directory}>{config.directory}</span>
      )}
    </BaseNode>
  )
}
