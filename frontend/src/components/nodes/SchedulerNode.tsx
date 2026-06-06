import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

export function SchedulerNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <BaseNode label={data.label as string} icon="🕐"
      colorCls="border-purple-500" isSource selected={selected}>
      {config.cron && (
        <span className="font-mono" title={config.cron}>{config.cron}</span>
      )}
    </BaseNode>
  )
}
