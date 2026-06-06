export type ConnectorCategory = 'TRIGGER' | 'PROCESSOR' | 'TRANSFORMER' | 'ACTION'

export interface ConfigField {
  key:          string
  label:        string
  type:         'string' | 'select' | 'number' | 'boolean' | 'textarea'
  required:     boolean
  defaultValue: string | number | boolean | null
  options:      string[] | null
}

export interface ConnectorDescriptor {
  type:         string
  label:        string
  category:     ConnectorCategory
  description:  string
  configFields: ConfigField[]
}
