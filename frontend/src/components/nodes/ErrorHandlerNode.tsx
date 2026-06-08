import { Handle, Position } from '@xyflow/react'
import type { NodeProps } from '@xyflow/react'

/** Standalone config node — drop anywhere, no connections required. */
export function ErrorHandlerNode({ data, selected }: NodeProps) {
  const config = (data.config ?? {}) as Record<string, string>
  return (
    <div className={`
      min-w-[180px] rounded-lg border-2 border-dashed bg-white shadow-md border-red-400
      ${selected ? 'ring-2 ring-offset-1 ring-blue-400' : ''}
    `}>
      {/* No input/output handles — standalone config node */}
      <div className="flex items-center gap-2 px-3 py-2">
        <span className="text-lg">🛡</span>
        <span className="text-sm font-semibold">{data.label as string}</span>
      </div>
      <div className="px-3 pb-2 border-t border-gray-100 pt-1 text-xs text-gray-500 space-y-0.5">
        <div>Retries: <span className="font-medium">{config.maxRedeliveries ?? '0'}</span></div>
        {config.useDeadLetter === 'true' && (
          <div className="text-red-500 truncate" title={config.deadLetterDir}>
            DLQ: {config.deadLetterDir ?? '/tmp/milan-deadletter'}
          </div>
        )}
      </div>
    </div>
  )
}
