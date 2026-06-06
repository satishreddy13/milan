import client from './client'
import type { Flow, CreateFlowRequest, UpdateFlowRequest } from '../types/flow'

export const flowsApi = {
  list:   ()                                  => client.get<Flow[]>('/flows').then(r => r.data),
  get:    (id: string)                        => client.get<Flow>(`/flows/${id}`).then(r => r.data),
  create: (req: CreateFlowRequest)            => client.post<Flow>('/flows', req).then(r => r.data),
  update: (id: string, req: UpdateFlowRequest) => client.put<Flow>(`/flows/${id}`, req).then(r => r.data),
  delete: (id: string)                        => client.delete(`/flows/${id}`),
  start:  (id: string)                        => client.post(`/flows/${id}/start`),
  stop:   (id: string)                        => client.post(`/flows/${id}/stop`),
}
