import type { NodeProps } from '@xyflow/react'
import { BaseNode } from './BaseNode'

const strategyLabel: Record<string, string> = {
  collect:     'Collect → JSON array',
  concatenate: 'Concatenate',
  latest:      'Keep latest',
}

export function AggregatorNode({ data, selected }: NodeProps) {
  const config  = (data.config ?? {}) as Record<string, string>
  const size    = config.completionSize    ? `${config.completionSize} msgs` : ''
  const timeout = config.completionTimeout ? `${config.completionTimeout} ms` : ''
  const when    = [size, timeout].filter(Boolean).join(' or ') || 'auto'

  const flags: string[] = []
  if (config.forceCompletionOnStop     === 'true') flags.push('force-on-stop')
  if (config.discardOnCompletionTimeout === 'true') flags.push('discard-on-timeout')

  return (
    <BaseNode label={data.label as string} icon="🗃"
      colorCls="border-fuchsia-500" selected={selected}>
      <span className="text-fuchsia-700 font-medium">
        {strategyLabel[config.strategy ?? 'collect'] ?? config.strategy}
      </span>
      <span className="block text-gray-400">Complete: {when}</span>
      {flags.length > 0 && (
        <span className="block text-gray-400 text-xs">{flags.join(', ')}</span>
      )}
    </BaseNode>
  )
}
