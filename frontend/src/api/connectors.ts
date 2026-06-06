import client from './client'
import type { ConnectorDescriptor } from '../types/connector'

export const connectorsApi = {
  list: () => client.get<ConnectorDescriptor[]>('/connectors').then(r => r.data),
}
