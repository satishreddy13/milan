import type { ConnectorDescriptor } from '../../types/connector'

interface Props {
  connectors: ConnectorDescriptor[]
}

const categoryColors: Record<string, string> = {
  TRIGGER:     'border-green-500  bg-green-50  text-green-800',
  ACTION:      'border-orange-500 bg-orange-50 text-orange-800',
  PROCESSOR:   'border-blue-500   bg-blue-50   text-blue-800',
  TRANSFORMER: 'border-purple-500 bg-purple-50 text-purple-800',
}

const categoryIcons: Record<string, string> = {
  TRIGGER:     '🌐',
  ACTION:      '📤',
  PROCESSOR:   '📋',
  TRANSFORMER: '✏️',
}

export function ConnectorPalette({ connectors }: Props) {
  const onDragStart = (e: React.DragEvent, connector: ConnectorDescriptor) => {
    e.dataTransfer.setData('application/milan-connector', JSON.stringify(connector))
    e.dataTransfer.effectAllowed = 'move'
  }

  return (
    <aside className="w-52 bg-white border-r border-gray-200 p-3 flex flex-col gap-2 overflow-y-auto">
      <h2 className="text-xs font-bold text-gray-500 uppercase tracking-wide mb-1">Connectors</h2>
      {connectors.map(c => (
        <div
          key={c.type}
          draggable
          onDragStart={e => onDragStart(e, c)}
          className={`
            flex items-center gap-2 px-3 py-2 rounded-lg border-2 cursor-grab
            text-sm font-medium select-none
            ${categoryColors[c.category] ?? 'border-gray-400 bg-gray-50 text-gray-700'}
          `}
          title={c.description}
        >
          <span>{categoryIcons[c.category] ?? '🔧'}</span>
          <span>{c.label}</span>
        </div>
      ))}
    </aside>
  )
}
