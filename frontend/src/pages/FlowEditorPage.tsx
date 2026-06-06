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
  const [connectors, setConnectors]   = useState<ConnectorDescriptor[]>([])
  const [saving, setSaving]           = useState(false)
  const [starting, setStarting]       = useState(false)

  const { currentFlow, nodes, edges, selectedNode,
          setCurrentFlow, setNodes, setEdges, setSelectedNode } = useFlowStore()

  // Load flow + connectors
  useEffect(() => {
    if (!id) return
    Promise.all([flowsApi.get(id), connectorsApi.list()]).then(([flow, conns]) => {
      setCurrentFlow(flow)
      setConnectors(conns)

      // Hydrate canvas from stored definition
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
            className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg
                       hover:bg-gray-50 disabled:opacity-50">
            {saving ? 'Saving…' : '💾 Save'}
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
              <NodeConfigPanel
                node={selectedNode}
                connectors={connectors}
              />
            )}
          </div>

          {id && <ExecutionLogFooter flowId={id} />}
        </main>
      </div>
    </div>
  )
}
