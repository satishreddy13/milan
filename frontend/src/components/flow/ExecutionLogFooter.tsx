import { useState, useEffect } from 'react'
import { logsApi } from '../../api/logs'
import type { ExecutionLog } from '../../types/log'

interface Props {
  flowId: string
}

const levelColors: Record<string, string> = {
  INFO:  'text-blue-600',
  WARN:  'text-yellow-600',
  ERROR: 'text-red-600',
}

export function ExecutionLogFooter({ flowId }: Props) {
  const [logs, setLogs]       = useState<ExecutionLog[]>([])
  const [open, setOpen]       = useState(true)
  const [loading, setLoading] = useState(false)

  const refresh = async () => {
    setLoading(true)
    try {
      setLogs(await logsApi.get(flowId, 50))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { refresh() }, [flowId])

  return (
    <div className="border-t border-gray-200 bg-gray-950 text-gray-200">
      {/* header */}
      <div className="flex items-center justify-between px-4 py-1.5 border-b border-gray-700">
        <button onClick={() => setOpen(o => !o)}
          className="text-xs font-mono text-gray-300 hover:text-white">
          {open ? '▼' : '▶'} Execution Logs
        </button>
        <button onClick={refresh}
          className="text-xs text-gray-400 hover:text-white">
          {loading ? '↻ loading…' : '↻ refresh'}
        </button>
      </div>

      {open && (
        <div className="h-36 overflow-y-auto px-4 py-2 font-mono text-xs space-y-0.5">
          {logs.length === 0 ? (
            <span className="text-gray-500">No logs yet. Start the flow and trigger it.</span>
          ) : (
            logs.map(l => (
              <div key={l.id} className="flex gap-3">
                <span className="text-gray-500 shrink-0">
                  {new Date(l.createdAt).toLocaleTimeString()}
                </span>
                <span className={`shrink-0 ${levelColors[l.level] ?? 'text-gray-300'}`}>
                  [{l.level}]
                </span>
                {l.nodeId && (
                  <span className="text-gray-500 shrink-0">{l.nodeId}</span>
                )}
                <span className="text-gray-200">{l.message}</span>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}
