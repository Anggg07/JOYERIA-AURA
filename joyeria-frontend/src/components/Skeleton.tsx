export default function Skeleton({ className = '', count = 1 }: { className?: string; count?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className={`animate-pulse bg-surface-elevated rounded ${className}`} />
      ))}
    </div>
  )
}

export function ProductCardSkeleton() {
  return (
    <div className="bg-surface rounded-lg border border-line/70 overflow-hidden">
      <div className="aspect-square animate-pulse bg-surface-elevated" />
      <div className="p-4 space-y-3">
        <div className="h-3 bg-surface-elevated rounded w-16 animate-pulse" />
        <div className="h-5 bg-surface-elevated rounded w-3/4 animate-pulse" />
        <div className="h-4 bg-surface-elevated rounded w-1/3 animate-pulse" />
      </div>
    </div>
  )
}
