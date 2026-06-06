import type { Node } from '@xyflow/react'
import type { ConnectorDescriptor } from '../../types/connector'
import { useFlowStore } from '../../store/flowStore'

interface Props {
  node:       Node
  connectors: ConnectorDescriptor[]
}

export function NodeConfigPanel({ node, connectors }: Props) {
  const updateNodeData = useFlowStore(s => s.updateNodeData)
  const descriptor     = connectors.find(c => c.type === node.type)
  const config         = (node.data.config ?? {}) as Record<string, string>

  if (!descriptor) return (
    <aside className="w-64 bg-white border-l border-gray-200 p-4">
      <p className="text-sm text-gray-500">Unknown connector: {node.type}</p>
    </aside>
  )

  const handleChange = (key: string, value: string) => {
    updateNodeData(node.id, { ...config, [key]: value })
  }

  return (
    <aside className="w-64 bg-white border-l border-gray-200 p-4 overflow-y-auto">
      <h2 className="text-sm font-bold text-gray-700 mb-1">{descriptor.label}</h2>
      <p className="text-xs text-gray-400 mb-4">{descriptor.description}</p>

      <div className="flex flex-col gap-3">
        {descriptor.configFields.map(field => (
          <div key={field.key}>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              {field.label}
              {field.required && <span className="text-red-500 ml-1">*</span>}
            </label>

            {field.type === 'select' ? (
              <select
                value={config[field.key] ?? String(field.defaultValue ?? '')}
                onChange={e => handleChange(field.key, e.target.value)}
                className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-400"
              >
                {field.options?.map(opt => (
                  <option key={opt} value={opt}>{opt}</option>
                ))}
              </select>
            ) : field.type === 'textarea' ? (
              <textarea
                rows={3}
                value={config[field.key] ?? String(field.defaultValue ?? '')}
                onChange={e => handleChange(field.key, e.target.value)}
                className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm font-mono focus:outline-none focus:ring-1 focus:ring-blue-400"
              />
            ) : (
              <input
                type="text"
                value={config[field.key] ?? String(field.defaultValue ?? '')}
                onChange={e => handleChange(field.key, e.target.value)}
                placeholder={String(field.defaultValue ?? '')}
                className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-400"
              />
            )}
          </div>
        ))}
      </div>
    </aside>
  )
}
