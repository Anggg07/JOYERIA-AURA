export const FREE_SHIPPING_THRESHOLD = 500000
export const SHIPPING_COST = 15000

export function calculateShipping(subtotal: number): number {
  return subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_COST
}
