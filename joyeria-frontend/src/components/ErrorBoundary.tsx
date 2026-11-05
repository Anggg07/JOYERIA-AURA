import { Component, type ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: { componentStack?: string | null }) {
    console.error('ErrorBoundary caught:', error, info.componentStack ?? '')
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center px-4 bg-background text-foreground">
          <div className="max-w-md text-center">
            <p className="text-[11px] uppercase tracking-[0.2em] text-[#C9A227] font-medium mb-3">Error</p>
            <h1 className="font-serif text-2xl font-medium mb-3">Algo salió mal</h1>
            <p className="text-sm text-foreground-muted mb-6">
              Ocurrió un error inesperado. Puedes recargar la página para intentar de nuevo.
            </p>
            <button
              onClick={() => window.location.reload()}
              className="bg-foreground text-background px-5 py-2.5 rounded-lg text-sm font-medium hover:opacity-90 transition"
            >
              Recargar página
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
