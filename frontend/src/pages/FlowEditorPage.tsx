import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { Node } from '@xyflow/react'
import { flowsApi }        from '../api/flows'
import { connectorsApi }   from '../api/connectors'
import { useFlowStore }    from '../store/flowStore'
import { ConnectorPalette } from '../components/flow/ConnectorPalette'
import { FlowCanvas }       from '../components/flow/FlowCanvas'
import { NodeConfigPanel }  from '../components/flow/NodeConfigPanel'
import { ExecutionLogFooter } from '../components/flow/ExecutionLogFooter'
import type { ConnectorDescriptor } from '../types/connector'
import type { FlowDefinition }      from '../types/flow'

export function FlowEditorPage() {
  const { id }          = useParams<{ id: string }>()
  const navigate        = useNavigate()
  const [connectors, setConnectors]     = useState<ConnectorDescriptor[]>([])
  const [saving, setSaving]             = useState(false)
  const [starting, setStarting]         = useState(false)
  const [showTrigger, setShowTrigger]   = useState(false)
  const [triggerBody, setTriggerBody]   = useState('{\n  "key": "value"\n}')
  const [triggerResult, setTriggerResult] = useState<string | null>(null)
  const [triggering, setTriggering]     = useState(false)

  const { currentFlow, nodes, edges, selectedNode, dirty,
          setCurrentFlow, setNodes, setEdges, setSelectedNode, setDirty } = useFlowStore()

  useEffect(() => {
    if (!id) return
    Promise.all([flowsApi.get(id), connectorsApi.list()]).then(([flow, conns]) => {
      setCurrentFlow(flow)
      setConnectors(conns)
      const def: FlowDefinition = flow.definition
      const rfNodes: Node[] = def.nodes.map(n => ({
        id:       n.id,
        type:     n.type,
        position: n.position,
        data:     n.data as unknown as Record<string, unknown>,
      }))
      setNodes(rfNodes)
      setEdges(def.edges.map(e => ({ id: e.id, source: e.source, target: e.target })))
    })
  }, [id])

  const save = async () => {
    if (!id) return
    setSaving(true)
    try {
      const definition: FlowDefinition = {
        nodes: nodes.map(n => ({
          id:       n.id,
          type:     n.type!,
          position: n.position,
          data:     n.data as any,
        })),
        edges: edges.map(e => ({ id: e.id, source: e.source, target: e.target })),
      }
      const updated = await flowsApi.update(id, { definition })
      setCurrentFlow(updated)
      setDirty(false)
    } finally {
      setSaving(false)
    }
  }

  const toggleFlow = async () => {
    if (!id || !currentFlow) return
    setStarting(true)
    try {
      if (currentFlow.status === 'ACTIVE') {
        await flowsApi.stop(id)
      } else {
        await save()
        await flowsApi.start(id)
      }
      const updated = await flowsApi.get(id)
      setCurrentFlow(updated)
    } catch (err: any) {
      const msg = err?.response?.data || err?.message || 'Unknown error'
      alert(`Failed to start flow: ${msg}`)
    } finally {
      setStarting(false)
    }
  }

  const sendTrigger = async () => {
    if (!id) return
    setTriggering(true)
    setTriggerResult(null)
    try {
      const res = await flowsApi.trigger(id, triggerBody)
      setTriggerResult(res.data ?? '(empty response)')
    } catch (err: any) {
      const msg = err?.response?.data || err?.message || 'Unknown error'
      setTriggerResult(`Error: ${msg}`)
    } finally {
      setTriggering(false)
    }
  }

  const statusColors: Record<string, string> = {
    DRAFT:    'bg-gray-100 text-gray-600',
    ACTIVE:   'bg-green-100 text-green-700',
    INACTIVE: 'bg-yellow-100 text-yellow-700',
    ERROR:    'bg-red-100 text-red-700',
  }

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-gray-100">
      {/* Toolbar */}
      <header className="bg-white border-b border-gray-200 px-4 py-2 flex items-center gap-3 shrink-0">
        <button onClick={() => navigate('/')}
          className="text-gray-400 hover:text-gray-600 text-sm">← Back</button>
        <span className="text-gray-300">|</span>
        <h1 className="font-semibold text-gray-800 text-sm truncate">
          {currentFlow?.name ?? 'Loading…'}
        </h1>
        {currentFlow && (
          <span className={`text-xs px-2 py-0.5 rounded-full font-medium
            ${statusColors[currentFlow.status] ?? ''}`}>
            {currentFlow.status}
          </span>
        )}
        <div className="ml-auto flex items-center gap-2">
          <button onClick={save} disabled={saving}
            className={`px-3 py-1.5 text-sm rounded-lg disabled:opacity-50 transition-colors
              ${dirty
                ? 'bg-amber-500 text-white hover:bg-amber-600 border border-amber-500'
                : 'border border-gray-300 hover:bg-gray-50 text-gray-700'}`}>
            {saving ? 'Saving…' : dirty ? '💾 Save*' : '💾 Save'}
          </button>
          <button
            onClick={() => { setShowTrigger(true); setTriggerResult(null) }}
            disabled={currentFlow?.status !== 'ACTIVE'}
            title={currentFlow?.status !== 'ACTIVE' ? 'Start the flow first' : 'Send a test payload'}
            className="px-3 py-1.5 text-sm border border-indigo-300 text-indigo-600 rounded-lg
                       hover:bg-indigo-50 disabled:opacity-40 disabled:cursor-not-allowed">
            ⚡ Test
          </button>
          <button onClick={toggleFlow} disabled={starting || !currentFlow}
            className={`px-3 py-1.5 text-sm rounded-lg font-medium disabled:opacity-50
              ${currentFlow?.status === 'ACTIVE'
                ? 'bg-red-50 text-red-600 hover:bg-red-100 border border-red-200'
                : 'bg-green-600 text-white hover:bg-green-700'}`}>
            {starting ? '…' : currentFlow?.status === 'ACTIVE' ? '⏹ Stop' : '▶ Start'}
          </button>
        </div>
      </header>

      {/* Body: palette | canvas | config panel */}
      <div className="flex flex-1 overflow-hidden">
        <ConnectorPalette connectors={connectors} />

        <main className="flex-1 flex flex-col overflow-hidden">
          <div className="flex flex-1 overflow-hidden">
            <FlowCanvas connectors={connectors} />
            {selectedNode && (
              <NodeConfigPanel node={selectedNode} connectors={connectors} />
            )}
          </div>
          {id && <ExecutionLogFooter flowId={id} />}
        </main>
      </div>

      {/* Test / Manual Trigger Modal */}
      {showTrigger && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-[520px] flex flex-col max-h-[80vh]">
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200">
              <h2 className="font-semibold text-gray-800">⚡ Test Flow</h2>
              <button onClick={() => setShowTrigger(false)}
                className="text-gray-400 hover:text-gray-600 text-xl leading-none">×</button>
            </div>

            <div className="p-5 flex flex-col gap-4 overflow-y-auto">
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">
                  Request Body
                </label>
                <textarea
                  value={triggerBody}
                  onChange={e => setTriggerBody(e.target.value)}
                  rows={8}
                  className="w-full font-mono text-sm border border-gray-300 rounded-lg px-3 py-2
                             focus:outline-none focus:ring-2 focus:ring-indigo-400 resize-none"
                />
              </div>

              {triggerResult !== null && (
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Response</label>
                  <pre className={`text-xs rounded-lg px-3 py-2 whitespace-pre-wrap break-all
                    ${triggerResult.startsWith('Error:')
                      ? 'bg-red-50 text-red-700 border border-red-200'
                      : 'bg-gray-50 text-gray-700 border border-gray-200'}`}>
                    {triggerResult}
                  </pre>
                </div>
              )}
            </div>

            <div className="px-5 py-4 border-t border-gray-200 flex justify-end gap-2">
              <button onClick={() => setShowTrigger(false)}
                className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg">
                Close
              </button>
              <button onClick={sendTrigger} disabled={triggering}
                className="px-4 py-2 text-sm bg-indigo-600 text-white rounded-lg
                           hover:bg-indigo-700 disabled:opacity-50">
                {triggering ? 'Sending…' : '⚡ Send'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
