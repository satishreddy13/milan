import { Handle, Position } from '@xyflow/react'
import type { ReactNode } from 'react'

interface BaseNodeProps {
  label:     string
  icon:      ReactNode
  colorCls:  string   // Tailwind border/bg class pair
  isSource?: boolean  // true = no incoming handle
  selected?: boolean
  children?: ReactNode
}

export function BaseNode({ label, icon, colorCls, isSource, selected, children }: BaseNodeProps) {
  return (
    <div className={`
      min-w-[160px] rounded-lg border-2 bg-white shadow-md
      ${colorCls}
      ${selected ? 'ring-2 ring-offset-1 ring-blue-400' : ''}
    `}>
      {!isSource && (
        <Handle type="target" position={Position.Left}
          className="!w-3 !h-3 !bg-gray-400 !border-2 !border-white" />
      )}

      <div className="flex items-center gap-2 px-3 py-2">
        <span className="text-lg">{icon}</span>
        <span className="text-sm font-semibold truncate">{label}</span>
      </div>

      {children && (
        <div className="px-3 pb-2 text-xs text-gray-500 border-t border-gray-100 pt-1">
          {children}
        </div>
      )}

      <Handle type="source" position={Position.Right}
        className="!w-3 !h-3 !bg-gray-400 !border-2 !border-white" />
    </div>
  )
}
