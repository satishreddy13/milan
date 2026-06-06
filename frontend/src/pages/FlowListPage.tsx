import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { flowsApi } from '../api/flows'
import type { Flow } from '../types/flow'

const statusColors: Record<string, string> = {
  DRAFT:    'bg-gray-100   text-gray-600',
  ACTIVE:   'bg-green-100  text-green-700',
  INACTIVE: 'bg-yellow-100 text-yellow-700',
  ERROR:    'bg-red-100    text-red-700',
}

export function FlowListPage() {
  const navigate            = useNavigate()
  const [flows, setFlows]   = useState<Flow[]>([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [newName, setNewName]   = useState('')

  const load = async () => {
    setLoading(true)
    try { setFlows(await flowsApi.list()) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const create = async () => {
    if (!newName.trim()) return
    const flow = await flowsApi.create({ name: newName.trim(), description: '' })
    navigate(`/flows/${flow.id}`)
  }

  const toggle = async (flow: Flow, e: React.MouseEvent) => {
    e.stopPropagation()
    if (flow.status === 'ACTIVE') await flowsApi.stop(flow.id)
    else                          await flowsApi.start(flow.id)
    load()
  }

  const del = async (flow: Flow, e: React.MouseEvent) => {
    e.stopPropagation()
    if (!confirm(`Delete "${flow.name}"?`)) return
    await flowsApi.delete(flow.id)
    load()
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 px-8 py-4 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Milan</h1>
          <p className="text-sm text-gray-500">Integration Platform</p>
        </div>
        <button onClick={() => setCreating(true)}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700">
          + New Flow
        </button>
      </header>

      {/* Create modal */}
      {creating && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl p-6 w-96">
            <h2 className="text-lg font-bold mb-4">New Integration Flow</h2>
            <input
              autoFocus
              value={newName}
              onChange={e => setNewName(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && create()}
              placeholder="Flow name…"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm mb-4
                         focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
            <div className="flex gap-2 justify-end">
              <button onClick={() => setCreating(false)}
                className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg">
                Cancel
              </button>
              <button onClick={create}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                Create
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Flow list */}
      <main className="max-w-5xl mx-auto px-8 py-8">
        {loading ? (
          <p className="text-gray-400 text-sm">Loading…</p>
        ) : flows.length === 0 ? (
          <div className="text-center py-24 text-gray-400">
            <p className="text-5xl mb-4">⚡</p>
            <p className="text-lg font-medium">No flows yet</p>
            <p className="text-sm">Click "New Flow" to create your first integration.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {flows.map(flow => (
              <div key={flow.id}
                onClick={() => navigate(`/flows/${flow.id}`)}
                className="bg-white rounded-xl border border-gray-200 px-6 py-4
                           flex items-center justify-between cursor-pointer
                           hover:shadow-md hover:border-blue-300 transition-all">
                <div>
                  <div className="flex items-center gap-3">
                    <h3 className="font-semibold text-gray-900">{flow.name}</h3>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium
                      ${statusColors[flow.status] ?? 'bg-gray-100 text-gray-500'}`}>
                      {flow.status}
                    </span>
                  </div>
                  {flow.description && (
                    <p className="text-sm text-gray-500 mt-0.5">{flow.description}</p>
                  )}
                  <p className="text-xs text-gray-400 mt-1">
                    Updated {new Date(flow.updatedAt).toLocaleString()}
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={e => toggle(flow, e)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium
                      ${flow.status === 'ACTIVE'
                        ? 'bg-red-50 text-red-600 hover:bg-red-100'
                        : 'bg-green-50 text-green-700 hover:bg-green-100'}`}>
                    {flow.status === 'ACTIVE' ? '⏹ Stop' : '▶ Start'}
                  </button>
                  <button
                    onClick={e => del(flow, e)}
                    className="px-3 py-1.5 rounded-lg text-xs text-gray-400 hover:bg-red-50 hover:text-red-500">
                    🗑
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
