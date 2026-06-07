import { createContext, useContext, useState, useCallback, useRef } from 'react'

type ToastType = 'success' | 'error' | 'info'

interface Toast {
  id:      number
  type:    ToastType
  message: string
}

interface ToastContextValue {
  toast: (message: string, type?: ToastType) => void
}

const ToastContext = createContext<ToastContextValue>({ toast: () => {} })

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const counter = useRef(0)

  const toast = useCallback((message: string, type: ToastType = 'info') => {
    const id = ++counter.current
    setToasts(prev => [...prev, { id, type, message }])
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4500)
  }, [])

  const styles: Record<ToastType, string> = {
    success: 'bg-green-600',
    error:   'bg-red-600',
    info:    'bg-gray-800',
  }

  const icons: Record<ToastType, string> = {
    success: '✓',
    error:   '✕',
    info:    'ℹ',
  }

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="fixed top-4 right-4 z-[200] flex flex-col gap-2 pointer-events-none">
        {toasts.map(t => (
          <div key={t.id}
            className={`flex items-start gap-2 px-4 py-3 rounded-lg shadow-lg text-sm text-white
                        max-w-sm animate-fade-in pointer-events-auto ${styles[t.type]}`}>
            <span className="font-bold shrink-0">{icons[t.type]}</span>
            <span className="break-words">{t.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
