export function formatPrice(price: number): string {
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(price).replace(/\s/g, ' ')
}

export function formatDate(date: string): string {
  return new Date(date).toLocaleDateString('es-CO', {
    year: 'numeric', month: 'long', day: 'numeric'
  })
}

export function getStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    PENDING: 'Pendiente', PAID: 'Pagado', PREPARING: 'Preparando',
    SHIPPED: 'Enviado', DELIVERED: 'Entregado', CANCELLED: 'Cancelado',
    OPEN: 'Abierto', IN_PROGRESS: 'En progreso', RESOLVED: 'Resuelto', CLOSED: 'Cerrado',
    APPROVED: 'Aprobada', REJECTED: 'Rechazada',
  }
  return labels[status] || status
}

export function getStatusColor(status: string): string {
  const colors: Record<string, string> = {
    PENDING: 'bg-yellow-100 text-yellow-800', PAID: 'bg-green-100 text-green-800',
    PREPARING: 'bg-blue-100 text-blue-800', SHIPPED: 'bg-purple-100 text-purple-800',
    DELIVERED: 'bg-green-100 text-green-800', CANCELLED: 'bg-red-100 text-red-800',
    OPEN: 'bg-yellow-100 text-yellow-800', IN_PROGRESS: 'bg-blue-100 text-blue-800',
    RESOLVED: 'bg-green-100 text-green-800', CLOSED: 'bg-gray-100 text-gray-800',
  }
  return colors[status] || 'bg-gray-100 text-gray-800'
}

export function generateSlug(text: string): string {
  return text.toLowerCase()
    .replace(/[áàäâ]/g, 'a').replace(/[éèëê]/g, 'e')
    .replace(/[íìïî]/g, 'i').replace(/[óòöô]/g, 'o')
    .replace(/[úùüû]/g, 'u').replace(/[ñ]/g, 'n')
    .replace(/[^a-z0-9\s-]/g, '').replace(/\s+/g, '-').replace(/-+/g, '-')
}