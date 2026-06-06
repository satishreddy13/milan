import { useCallback, useRef } from 'react'
import {
  ReactFlow, Background, Controls, MiniMap,
  type Node, type ReactFlowInstance,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

import { useFlowStore } from '../../store/flowStore'
import { HttpListenerNode } from '../nodes/HttpListenerNode'
import { HttpRequestNode }  from '../nodes/HttpRequestNode'
import { LoggerNode }       from '../nodes/LoggerNode'
import { SetBodyNode }      from '../nodes/SetBodyNode'
import type { ConnectorDescriptor } from '../../types/connector'

// Must be defined OUTSIDE the component to avoid remounting on every render
const nodeTypes = {
  HTTP_LISTENER: HttpListenerNode,
  HTTP_REQUEST:  HttpRequestNode,
  LOGGER:        LoggerNode,
  SET_BODY:      SetBodyNode,
}

interface Props {
  connectors: ConnectorDescriptor[]
}

let nodeCounter = 0

export function FlowCanvas({ connectors }: Props) {
  const { nodes, edges, onNodesChange, onEdgesChange, onConnect,
          setSelectedNode, selectedNode } = useFlowStore()

  const rfInstance = useRef<ReactFlowInstance | null>(null)
  const wrapperRef = useRef<HTMLDivElement>(null)

  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    const raw = e.dataTransfer.getData('application/milan-connector')
    if (!raw || !rfInstance.current || !wrapperRef.current) return

    const descriptor: ConnectorDescriptor = JSON.parse(raw)
    const rect   = wrapperRef.current.getBoundingClientRect()
    const pos    = rfInstance.current.screenToFlowPosition({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    })

    const defaults: Record<string, string> = {}
    descriptor.configFields.forEach(f => {
      defaults[f.key] = String(f.defaultValue ?? '')
    })

    const newNode: Node = {
      id:       `${descriptor.type}-${++nodeCounter}`,
      type:     descriptor.type,
      position: pos,
      data:     { label: descriptor.label, config: defaults },
    }

    useFlowStore.getState().setNodes([...useFlowStore.getState().nodes, newNode])
  }, [])

  const onDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }, [])

  return (
    <div ref={wrapperRef} className="flex-1 h-full">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onInit={inst => { rfInstance.current = inst }}
        onNodeClick={(_, node) => setSelectedNode(node)}
        onPaneClick={() => setSelectedNode(null)}
        onDrop={onDrop}
        onDragOver={onDragOver}
        fitView
        deleteKeyCode="Delete"
      >
        <Background />
        <Controls />
        <MiniMap />
      </ReactFlow>
    </div>
  )
}
