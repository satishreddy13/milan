export type FlowStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'ERROR'

export interface FlowDefinition {
  nodes: MilanNode[]
  edges: MilanEdge[]
}

export interface MilanNode {
  id:       string
  type:     string       // connector type: HTTP_LISTENER | HTTP_REQUEST | LOGGER | SET_BODY
  position: { x: number; y: number }
  data:     MilanNodeData
}

export interface MilanNodeData {
  label:  string
  config: Record<string, string>
}

export interface MilanEdge {
  id:            string
  source:        string
  target:        string
  sourceHandle?: string   // which output handle the edge came from (e.g. "when" / "otherwise")
}

export interface Flow {
  id:          string
  name:        string
  description: string | null
  status:      FlowStatus
  definition:  FlowDefinition
  createdAt:   string
  updatedAt:   string
}

export interface CreateFlowRequest {
  name:        string
  description: string
}

export interface UpdateFlowRequest {
  name?:        string
  description?: string
  definition?:  FlowDefinition
}
