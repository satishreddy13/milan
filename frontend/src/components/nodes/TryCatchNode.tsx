import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

const actionLabel: Record<string, string> = {
  log:        'Log & continue',
  deadLetter: 'Dead letter',
  rethrow:    'Rethrow',
}

export function TryCatchNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  const action = config.errorAction ?? 'log'
  return (
    <BaseNode label={data.label as string} icon="🔒"
      colorCls="border-rose-500" selected={selected}>
      <span className="text-rose-600 font-medium">{actionLabel[action] ?? action}</span>
      {action === 'deadLetter' && config.deadLetterDir && (
        <span className="block truncate text-gray-400 max-w-[140px]"
              title={config.deadLetterDir}>{config.deadLetterDir}</span>
      )}
    </BaseNode>
  )
}
