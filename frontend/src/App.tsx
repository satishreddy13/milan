import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { FlowListPage }   from './pages/FlowListPage'
import { FlowEditorPage } from './pages/FlowEditorPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/"           element={<FlowListPage />} />
        <Route path="/flows/:id"  element={<FlowEditorPage />} />
        <Route path="*"           element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
