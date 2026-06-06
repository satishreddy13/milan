import type { Node } from '@xyflow/react'
import type { ConnectorDescriptor } from '../../types/connector'
import { useFlowStore } from '../../store/flowStore'
import cronstrue from 'cronstrue'

function parseCron(raw: string): { ok: boolean; text: string } {
  const expr = raw.trim()
  if (!expr) return { ok: false, text: '' }
  // Accept both 5-field (Unix) and 6-field (Quartz) by normalising to 6-field
  const normalized = expr.split(/\s+/).length === 5 ? '0 ' + expr : expr
  try {
    const text = cronstrue.toString(normalized, { use24HourTimeFormat: true, throwExceptionOnParseError: true })
    return { ok: true, text }
  } catch {
    return { ok: false, text: 'Invalid cron expression' }
  }
}

interface Props {
  node:       Node
  connectors: ConnectorDescriptor[]
}

export function NodeConfigPanel({ node, connectors }: Props) {
  const updateNodeData = useFlowStore(s => s.updateNodeData)
  const deleteNode     = useFlowStore(s => s.deleteNode)

  // Read config from the live store so edits reflect immediately
  const liveNode   = useFlowStore(s => s.nodes.find(n => n.id === node.id)) ?? node
  const descriptor = connectors.find(c => c.type === liveNode.type)
  const config     = (liveNode.data.config ?? {}) as Record<string, string>

  if (!descriptor) return (
    <aside className="w-64 bg-white border-l border-gray-200 p-4">
      <p className="text-sm text-gray-500">Unknown connector: {liveNode.type}</p>
    </aside>
  )

  const handleChange = (key: string, value: string) => {
    updateNodeData(liveNode.id, { ...config, [key]: value })
  }

  return (
    <aside className="w-64 bg-white border-l border-gray-200 flex flex-col overflow-hidden">
      <div className="p-4 flex-1 overflow-y-auto">
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
                  className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm
                             focus:outline-none focus:ring-1 focus:ring-blue-400"
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
                  className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm
                             font-mono focus:outline-none focus:ring-1 focus:ring-blue-400"
                />
              ) : field.type === 'cron' ? (() => {
                const raw = config[field.key] ?? String(field.defaultValue ?? '')
                const { ok, text } = parseCron(raw)
                return (
                  <>
                    <input
                      type="text"
                      value={raw}
                      onChange={e => handleChange(field.key, e.target.value)}
                      placeholder={String(field.defaultValue ?? '')}
                      className={`w-full border rounded px-2 py-1.5 text-sm font-mono
                                  focus:outline-none focus:ring-1
                                  ${ok
                                    ? 'border-gray-300 focus:ring-blue-400'
                                    : 'border-red-400 focus:ring-red-400'}`}
                    />
                    {text && (
                      <p className={`mt-1 text-xs ${ok ? 'text-green-600' : 'text-red-500'}`}>
                        {ok ? `✓ ${text}` : `✗ ${text}`}
                      </p>
                    )}
                  </>
                )
              })() : (
                <input
                  type="text"
                  value={config[field.key] ?? String(field.defaultValue ?? '')}
                  onChange={e => handleChange(field.key, e.target.value)}
                  placeholder={String(field.defaultValue ?? '')}
                  className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm
                             focus:outline-none focus:ring-1 focus:ring-blue-400"
                />
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Delete button pinned to bottom */}
      <div className="p-3 border-t border-gray-100">
        <button
          onClick={() => deleteNode(liveNode.id)}
          className="w-full px-3 py-1.5 text-xs text-red-500 border border-red-200
                     rounded-lg hover:bg-red-50 transition-colors">
          🗑 Delete node
        </button>
      </div>
    </aside>
  )
}
