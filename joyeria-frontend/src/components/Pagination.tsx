import { ChevronLeft, ChevronRight } from 'lucide-react'
import Button from './Button'

interface Props { page: number; totalPages: number; onPageChange: (p: number) => void }

export default function Pagination({ page, totalPages, onPageChange }: Props) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-center gap-2 mt-8">
      <Button variant="ghost" size="sm" onClick={() => onPageChange(page - 1)} disabled={page === 0}>
        <ChevronLeft size={16} />
      </Button>
      {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
        const start = Math.max(0, Math.min(page - 2, totalPages - 5))
        const p = start + i
        return (
          <button key={p} onClick={() => onPageChange(p)}
            className={`w-11 h-11 rounded text-sm font-medium transition ${p === page ? 'bg-[#C9A227] text-white' : 'text-foreground-muted hover:bg-surface-muted'}`}>
            {p + 1}
          </button>
        )
      })}
      <Button variant="ghost" size="sm" onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}>
        <ChevronRight size={16} />
      </Button>
    </div>
  )
}
