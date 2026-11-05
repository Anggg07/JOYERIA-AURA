import { Star } from 'lucide-react'
import type { Review } from '../types'
import { formatDate } from '../utils/format'

interface Props { review: Review }

export default function ReviewCard({ review }: Props) {
  return (
    <div className="border-b border-line/70 py-5 last:border-0">
      <div className="flex items-center gap-2 mb-2">
        {[1, 2, 3, 4, 5].map(i => (
          <Star key={i} size={14} className={i <= review.rating ? 'text-[#C9A227] fill-[#C9A227]' : 'text-foreground/20'} />
        ))}
        <span className="text-sm font-medium text-foreground">{review.userName}</span>
      </div>
      {review.title && <p className="font-medium text-sm text-foreground mb-1">{review.title}</p>}
      {review.comment && <p className="text-sm text-foreground-muted leading-relaxed">{review.comment}</p>}
      <p className="text-xs text-foreground-faint mt-2">{formatDate(review.createdAt)}</p>
    </div>
  )
}
