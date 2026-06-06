import { create } from 'zustand'
import { applyNodeChanges, applyEdgeChanges, addEdge } from '@xyflow/react'
import type { Node, Edge, NodeChange, EdgeChange, Connection } from '@xyflow/react'
import type { Flow } from '../types/flow'

interface FlowStore {
  // Canvas state
  nodes:        Node[]
  edges:        Edge[]
  selectedNode: Node | null

  // Persisted flow metadata
  currentFlow:  Flow | null

  setCurrentFlow:  (flow: Flow) => void
  setNodes:        (nodes: Node[]) => void
  setEdges:        (edges: Edge[]) => void
  setSelectedNode: (node: Node | null) => void

  onNodesChange: (changes: NodeChange[]) => void
  onEdgesChange: (changes: EdgeChange[]) => void
  onConnect:     (connection: Connection) => void

  updateNodeData: (nodeId: string, config: Record<string, string>) => void
}

export const useFlowStore = create<FlowStore>((set, get) => ({
  nodes:        [],
  edges:        [],
  selectedNode: null,
  currentFlow:  null,

  setCurrentFlow:  (flow) => set({ currentFlow: flow }),
  setNodes:        (nodes) => set({ nodes }),
  setEdges:        (edges) => set({ edges }),
  setSelectedNode: (node)  => set({ selectedNode: node }),

  onNodesChange: (changes) =>
    set({ nodes: applyNodeChanges(changes, get().nodes) }),

  onEdgesChange: (changes) =>
    set({ edges: applyEdgeChanges(changes, get().edges) }),

  onConnect: (connection) =>
    set({ edges: addEdge({ ...connection, animated: true }, get().edges) }),

  updateNodeData: (nodeId, config) =>
    set({
      nodes: get().nodes.map(n =>
        n.id === nodeId ? { ...n, data: { ...n.data, config } } : n
      ),
    }),
}))
