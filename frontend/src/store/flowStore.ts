import { create } from 'zustand'
import { applyNodeChanges, applyEdgeChanges, addEdge } from '@xyflow/react'
import type { Node, Edge, NodeChange, EdgeChange, Connection } from '@xyflow/react'
import type { Flow } from '../types/flow'

interface FlowStore {
  // Canvas state
  nodes:        Node[]
  edges:        Edge[]
  selectedNode: Node | null
  dirty:        boolean       // true when canvas has unsaved changes

  // Persisted flow metadata
  currentFlow:  Flow | null

  setCurrentFlow:  (flow: Flow) => void
  setNodes:        (nodes: Node[]) => void   // resets dirty
  setEdges:        (edges: Edge[]) => void   // resets dirty
  setSelectedNode: (node: Node | null) => void
  setDirty:        (dirty: boolean) => void

  onNodesChange: (changes: NodeChange[]) => void
  onEdgesChange: (changes: EdgeChange[]) => void
  onConnect:     (connection: Connection) => void

  updateNodeData: (nodeId: string, config: Record<string, string>) => void
  deleteNode:     (nodeId: string) => void
}

export const useFlowStore = create<FlowStore>((set, get) => ({
  nodes:        [],
  edges:        [],
  selectedNode: null,
  dirty:        false,
  currentFlow:  null,

  setCurrentFlow:  (flow)  => set({ currentFlow: flow }),
  // Loading from backend — not dirty
  setNodes:        (nodes) => set({ nodes, dirty: false }),
  setEdges:        (edges) => set({ edges, dirty: false }),
  setSelectedNode: (node)  => set({ selectedNode: node }),
  setDirty:        (dirty) => set({ dirty }),

  onNodesChange: (changes) => {
    // 'dimensions' fires on init (React Flow measuring nodes), 'select' fires on click —
    // neither represents a user edit, so don't mark dirty for those.
    const userEdit = changes.some(c => c.type !== 'dimensions' && c.type !== 'select')
    set({ nodes: applyNodeChanges(changes, get().nodes), ...(userEdit && { dirty: true }) })
  },

  onEdgesChange: (changes) => {
    const userEdit = changes.some(c => c.type !== 'select')
    set({ edges: applyEdgeChanges(changes, get().edges), ...(userEdit && { dirty: true }) })
  },

  onConnect: (connection) =>
    set({ edges: addEdge({ ...connection, animated: true }, get().edges), dirty: true }),

  updateNodeData: (nodeId, config) => {
    const updatedNodes = get().nodes.map(n =>
      n.id === nodeId ? { ...n, data: { ...n.data, config } } : n
    )
    // Keep selectedNode in sync so NodeConfigPanel sees the latest config
    const updatedSelected = get().selectedNode?.id === nodeId
      ? (updatedNodes.find(n => n.id === nodeId) ?? null)
      : get().selectedNode
    set({ nodes: updatedNodes, selectedNode: updatedSelected, dirty: true })
  },

  deleteNode: (nodeId) => {
    set({
      nodes:        get().nodes.filter(n => n.id !== nodeId),
      edges:        get().edges.filter(e => e.source !== nodeId && e.target !== nodeId),
      selectedNode: get().selectedNode?.id === nodeId ? null : get().selectedNode,
      dirty:        true,
    })
  },
}))
