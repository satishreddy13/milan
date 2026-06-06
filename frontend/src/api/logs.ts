import client from './client'
import type { ExecutionLog } from '../types/log'

export const logsApi = {
  get: (flowId: string, limit = 100) =>
    client.get<ExecutionLog[]>(`/flows/${flowId}/logs`, { params: { limit } }).then(r => r.data),
}
