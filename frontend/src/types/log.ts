export interface ExecutionLog {
  id:        string
  flowId:    string
  nodeId:    string | null
  level:     'INFO' | 'WARN' | 'ERROR'
  message:   string
  createdAt: string
}
