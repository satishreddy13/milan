import { Handle, Position } from '@xyflow/react'
import type { NodeProps } from '@xyflow/react'

/**
 * Content-Based Router node.
 *
 * Handles:
 *   Left           — single input (target)
 *   Right, top ½   — "when" branch  (source, id="when")
 *   Right, bot ½   — "otherwise"    (source, id="otherwise")
 */
export function ChoiceNode({ data, selected }: NodeProps) {
  const config    = (data.config ?? {}) as Record<string, string>
  const condition = config.condition ?? ''

  return (
    <div className={`
      min-w-[190px] rounded-lg border-2 bg-white shadow-md border-yellow-500
      ${selected ? 'ring-2 ring-offset-1 ring-blue-400' : ''}
    `}>
      {/* Input handle */}
      <Handle type="target" position={Position.Left}
        className="!w-3 !h-3 !bg-gray-400 !border-2 !border-white" />

      {/* Header */}
      <div className="flex items-center gap-2 px-3 py-2">
        <span className="text-lg">🔀</span>
        <span className="text-sm font-semibold">{data.label as string}</span>
      </div>

      {/* Condition preview */}
      {condition && (
        <div className="px-3 pb-2 border-t border-gray-100 pt-1">
          <code className="text-xs text-yellow-700 font-mono break-all line-clamp-2"
                title={condition}>
            {condition}
          </code>
        </div>
      )}

      {/* Branch rows — handles are positioned relative to node via style.top */}
      <div className="border-t border-gray-100 text-xs">
        <div className="flex items-center justify-between px-3 py-1.5 border-b border-gray-100">
          <span className="font-medium text-green-600">when</span>
          <span className="text-green-400 text-[10px]">✓ true</span>
        </div>
        <div className="flex items-center justify-between px-3 py-1.5">
          <span className="font-medium text-gray-500">otherwise</span>
          <span className="text-gray-400 text-[10px]">✗ false</span>
        </div>
      </div>

      {/* "when" source handle — sits at ~75% of node height (first branch row) */}
      <Handle
        type="source"
        position={Position.Right}
        id="when"
        style={{ top: '72%' }}
        className="!w-3 !h-3 !bg-green-500 !border-2 !border-white"
      />

      {/* "otherwise" source handle — sits at ~88% of node height (second branch row) */}
      <Handle
        type="source"
        position={Position.Right}
        id="otherwise"
        style={{ top: '90%' }}
        className="!w-3 !h-3 !bg-gray-400 !border-2 !border-white"
      />
    </div>
  )
}
