import { useState, useRef } from 'react'

interface Props {
  value:       string
  onChange:    (v: string) => void
  placeholder?: string
}

// ── Variable reference data ──────────────────────────────────────────────────

const VARIABLE_GROUPS = [
  {
    label: 'Message',
    vars: [
      { expr: '${body}',              desc: 'Message body (as-is)' },
      { expr: '${bodyAs(String)}',    desc: 'Body converted to String' },
      { expr: '${exchangeId}',        desc: 'Unique exchange ID' },
      { expr: '${messageId}',         desc: 'Message ID' },
    ],
  },
  {
    label: 'File',
    vars: [
      { expr: '${header.CamelFileName}',         desc: 'File name (relative)' },
      { expr: '${header.CamelFileNameOnly}',      desc: 'File name without path' },
      { expr: '${header.CamelFileLength}',        desc: 'File size in bytes' },
      { expr: '${header.CamelFilePath}',          desc: 'Full file path' },
      { expr: '${header.CamelFileParent}',        desc: 'Parent directory' },
      { expr: '${header.CamelFileAbsolutePath}',  desc: 'Absolute file path' },
      { expr: '${header.CamelFileLastModified}',  desc: 'Last modified (ms)' },
    ],
  },
  {
    label: 'HTTP',
    vars: [
      { expr: '${header.CamelHttpMethod}',       desc: 'HTTP method' },
      { expr: '${header.CamelHttpPath}',         desc: 'Request path' },
      { expr: '${header.CamelHttpResponseCode}', desc: 'Response status code' },
      { expr: '${header.Content-Type}',          desc: 'Content-Type header' },
    ],
  },
  {
    label: 'Date / Time',
    vars: [
      { expr: '${date:now:yyyy-MM-dd}',              desc: 'Current date' },
      { expr: '${date:now:HH:mm:ss}',               desc: 'Current time' },
      { expr: '${date:now:yyyyMMdd-HHmmssSSS}',      desc: 'Timestamp (file-safe)' },
      { expr: '${date:now:yyyy-MM-dd HH:mm:ss}',     desc: 'Full timestamp' },
    ],
  },
  {
    label: 'Operators (use in conditions)',
    vars: [
      { expr: '== ',         desc: 'Equals' },
      { expr: '!= ',         desc: 'Not equals' },
      { expr: '> ',          desc: 'Greater than' },
      { expr: '>= ',         desc: 'Greater or equal' },
      { expr: '< ',          desc: 'Less than' },
      { expr: '<= ',         desc: 'Less or equal' },
      { expr: ' contains ',  desc: 'String contains' },
      { expr: ' regex ',     desc: 'Regex match (quote pattern)' },
      { expr: ' is null',    desc: 'Is null check' },
      { expr: ' is not null',desc: 'Not null check' },
    ],
  },
]

// ── Component ────────────────────────────────────────────────────────────────

export function ExpressionInput({ value, onChange, placeholder }: Props) {
  const [showVars, setShowVars] = useState(false)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const insertAt = (expr: string) => {
    const el = textareaRef.current
    if (!el) { onChange(value + expr); return }
    const start = el.selectionStart ?? value.length
    const end   = el.selectionEnd   ?? value.length
    const next  = value.slice(0, start) + expr + value.slice(end)
    onChange(next)
    // restore cursor after React re-render
    requestAnimationFrame(() => {
      el.focus()
      el.setSelectionRange(start + expr.length, start + expr.length)
    })
  }

  return (
    <div>
      <textarea
        ref={textareaRef}
        rows={3}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        spellCheck={false}
        className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm font-mono
                   focus:outline-none focus:ring-1 focus:ring-blue-400 resize-y"
      />

      {/* Toggle */}
      <button
        type="button"
        onClick={() => setShowVars(v => !v)}
        className="mt-1 text-xs text-blue-500 hover:text-blue-700 flex items-center gap-1">
        {showVars ? '▾' : '▸'} Variables reference
      </button>

      {/* Variable panel */}
      {showVars && (
        <div className="mt-1 border border-gray-200 rounded-lg bg-gray-50 max-h-56 overflow-y-auto text-xs">
          {VARIABLE_GROUPS.map(group => (
            <div key={group.label} className="border-b border-gray-100 last:border-0">
              <p className="px-2 py-1 font-semibold text-gray-500 bg-gray-100 sticky top-0">
                {group.label}
              </p>
              {group.vars.map(v => (
                <button
                  key={v.expr}
                  type="button"
                  onClick={() => insertAt(v.expr)}
                  title={`Insert: ${v.expr}`}
                  className="w-full text-left px-2 py-1 flex items-baseline gap-2
                             hover:bg-blue-50 transition-colors">
                  <code className="font-mono text-blue-700 shrink-0">{v.expr}</code>
                  <span className="text-gray-400 truncate">{v.desc}</span>
                </button>
              ))}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
