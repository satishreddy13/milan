import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { flowsApi } from '../api/flows'
import { useToast } from '../context/ToastContext'
import type { Flow } from '../types/flow'

const statusColors: Record<string, string> = {
  DRAFT:    'bg-gray-100   text-gray-600',
  ACTIVE:   'bg-green-100  text-green-700',
  INACTIVE: 'bg-yellow-100 text-yellow-700',
  ERROR:    'bg-red-100    text-red-700',
}

const statusDot: Record<string, string> = {
  ACTIVE:   'bg-green-500',
  INACTIVE: 'bg-yellow-400',
  ERROR:    'bg-red-500',
  DRAFT:    'bg-gray-400',
}

export function FlowListPage() {
  const navigate      = useNavigate()
  const { toast }     = useToast()
  const [flows, setFlows]           = useState<Flow[]>([])
  const [loading, setLoading]       = useState(true)
  const [creating, setCreating]     = useState(false)
  const [newName, setNewName]       = useState('')
  const [newDesc, setNewDesc]       = useState('')

  // Rename state
  const [renaming, setRenaming]     = useState<Flow | null>(null)
  const [renameVal, setRenameVal]   = useState('')
  const [renameDesc, setRenameDesc] = useState('')

  // Delete confirmation state
  const [deleting, setDeleting]     = useState<Flow | null>(null)

  const load = async () => {
    setLoading(true)
    try { setFlows(await flowsApi.list()) }
    catch { toast('Failed to load flows', 'error') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const create = async () => {
    if (!newName.trim()) return
    try {
      const flow = await flowsApi.create({ name: newName.trim(), description: newDesc.trim() })
      navigate(`/flows/${flow.id}`)
    } catch {
      toast('Failed to create flow', 'error')
    }
  }

  const rename = async () => {
    if (!renaming || !renameVal.trim()) return
    try {
      await flowsApi.update(renaming.id, { name: renameVal.trim(), description: renameDesc.trim() })
      toast('Flow renamed', 'success')
      setRenaming(null)
      load()
    } catch {
      toast('Failed to rename flow', 'error')
    }
  }

  const toggle = async (flow: Flow, e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      if (flow.status === 'ACTIVE') {
        await flowsApi.stop(flow.id)
        toast(`"${flow.name}" stopped`, 'info')
      } else {
        await flowsApi.start(flow.id)
        toast(`"${flow.name}" started`, 'success')
      }
      load()
    } catch (err: any) {
      const msg = err?.response?.data || err?.message || 'Unknown error'
      toast(`Failed: ${msg}`, 'error')
    }
  }

  const confirmDelete = async () => {
    if (!deleting) return
    try {
      await flowsApi.delete(deleting.id)
      toast(`"${deleting.name}" deleted`, 'info')
      setDeleting(null)
      load()
    } catch {
      toast('Failed to delete flow', 'error')
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 px-8 py-4 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Milan</h1>
          <p className="text-sm text-gray-500">Integration Platform</p>
        </div>
        <button onClick={() => { setNewName(''); setNewDesc(''); setCreating(true) }}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700">
          + New Flow
        </button>
      </header>

      {/* Create modal */}
      {creating && (
        <Modal title="New Integration Flow" onClose={() => setCreating(false)}>
          <div className="flex flex-col gap-3">
            <LabeledInput label="Flow name" required autoFocus
              value={newName} onChange={setNewName}
              onEnter={create} placeholder="e.g. Invoice Processor" />
            <LabeledInput label="Description (optional)"
              value={newDesc} onChange={setNewDesc}
              onEnter={create} placeholder="What does this flow do?" />
          </div>
          <ModalFooter onClose={() => setCreating(false)}
            onConfirm={create} confirmLabel="Create" confirmDisabled={!newName.trim()} />
        </Modal>
      )}

      {/* Rename modal */}
      {renaming && (
        <Modal title="Rename Flow" onClose={() => setRenaming(null)}>
          <div className="flex flex-col gap-3">
            <LabeledInput label="Flow name" required autoFocus
              value={renameVal} onChange={setRenameVal}
              onEnter={rename} placeholder="Flow name" />
            <LabeledInput label="Description (optional)"
              value={renameDesc} onChange={setRenameDesc}
              onEnter={rename} placeholder="What does this flow do?" />
          </div>
          <ModalFooter onClose={() => setRenaming(null)}
            onConfirm={rename} confirmLabel="Save" confirmDisabled={!renameVal.trim()} />
        </Modal>
      )}

      {/* Delete confirmation */}
      {deleting && (
        <Modal title="Delete Flow" onClose={() => setDeleting(null)}>
          <p className="text-sm text-gray-600">
            Are you sure you want to delete <strong>"{deleting.name}"</strong>?
            This cannot be undone.
          </p>
          <ModalFooter onClose={() => setDeleting(null)}
            onConfirm={confirmDelete} confirmLabel="Delete" danger />
        </Modal>
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
                <div className="flex items-center gap-4 min-w-0">
                  {/* Live status dot */}
                  <span className={`w-2.5 h-2.5 rounded-full shrink-0
                    ${statusDot[flow.status] ?? 'bg-gray-300'}
                    ${flow.status === 'ACTIVE' ? 'animate-pulse' : ''}`} />
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold text-gray-900 truncate">{flow.name}</h3>
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium shrink-0
                        ${statusColors[flow.status] ?? 'bg-gray-100 text-gray-500'}`}>
                        {flow.status}
                      </span>
                    </div>
                    {flow.description && (
                      <p className="text-sm text-gray-500 mt-0.5 truncate">{flow.description}</p>
                    )}
                    <p className="text-xs text-gray-400 mt-0.5">
                      Updated {new Date(flow.updatedAt).toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-1 shrink-0 ml-4"
                     onClick={e => e.stopPropagation()}>
                  <button
                    onClick={e => toggle(flow, e)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors
                      ${flow.status === 'ACTIVE'
                        ? 'bg-red-50 text-red-600 hover:bg-red-100'
                        : 'bg-green-50 text-green-700 hover:bg-green-100'}`}>
                    {flow.status === 'ACTIVE' ? '⏹ Stop' : '▶ Start'}
                  </button>
                  <button
                    title="Rename"
                    onClick={() => { setRenaming(flow); setRenameVal(flow.name); setRenameDesc(flow.description ?? '') }}
                    className="p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors text-sm">
                    ✏
                  </button>
                  <button
                    title="Delete"
                    onClick={() => setDeleting(flow)}
                    className="p-1.5 rounded-lg text-gray-400 hover:bg-red-50 hover:text-red-500 transition-colors text-sm">
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

// ─── Shared modal primitives ─────────────────────────────────────────────────

function Modal({ title, onClose, children }: {
  title:    string
  onClose:  () => void
  children: React.ReactNode
}) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
         onClick={onClose}>
      <div className="bg-white rounded-xl shadow-xl p-6 w-[420px] flex flex-col gap-4"
           onClick={e => e.stopPropagation()}>
        <h2 className="text-base font-bold text-gray-800">{title}</h2>
        {children}
      </div>
    </div>
  )
}

function ModalFooter({ onClose, onConfirm, confirmLabel, confirmDisabled, danger }: {
  onClose:          () => void
  onConfirm:        () => void
  confirmLabel:     string
  confirmDisabled?: boolean
  danger?:          boolean
}) {
  return (
    <div className="flex gap-2 justify-end pt-1">
      <button onClick={onClose}
        className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg">
        Cancel
      </button>
      <button onClick={onConfirm} disabled={confirmDisabled}
        className={`px-4 py-2 text-sm rounded-lg font-medium disabled:opacity-50
          ${danger
            ? 'bg-red-600 text-white hover:bg-red-700'
            : 'bg-blue-600 text-white hover:bg-blue-700'}`}>
        {confirmLabel}
      </button>
    </div>
  )
}

function LabeledInput({ label, value, onChange, onEnter, placeholder, required, autoFocus }: {
  label:        string
  value:        string
  onChange:     (v: string) => void
  onEnter?:     () => void
  placeholder?: string
  required?:    boolean
  autoFocus?:   boolean
}) {
  return (
    <div>
      <label className="block text-xs font-medium text-gray-600 mb-1">
        {label}{required && <span className="text-red-500 ml-0.5">*</span>}
      </label>
      <input
        autoFocus={autoFocus}
        value={value}
        onChange={e => onChange(e.target.value)}
        onKeyDown={e => e.key === 'Enter' && onEnter?.()}
        placeholder={placeholder}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm
                   focus:outline-none focus:ring-2 focus:ring-blue-400"
      />
    </div>
  )
}
