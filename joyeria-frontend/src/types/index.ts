export interface Product {
  id: number
  name: string
  slug: string
  description?: string
  price: number
  comparePrice?: number
  sku: string
  stock: number
  weight?: string
  dimensions?: string
  size?: string
  color?: string
  careInstructions?: string
  features?: string
  deliveryTime?: string
  featured: boolean
  isNew: boolean
  bestSeller: boolean
  active: boolean
  soldCount: number
  categoryId?: number
  categoryName?: string
  materialIds?: number[]
  materialNames?: string[]
  images: ProductImage[]
  discountPercent: number
  averageRating?: number
  reviewCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface ProductImage {
  id: number
  url: string
  alt?: string
  isPrimary: boolean
  sortOrder: number
}

export interface Category {
  id: number
  name: string
  slug: string
  description?: string
  displayOrder?: number
  image?: string
  active: boolean
  productCount?: number
}

export interface Material {
  id: number
  name: string
  slug: string
  description?: string
  image?: string
  active: boolean
}

export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  phone?: string
  role: 'USER' | 'ADMIN'
  active: boolean
  createdAt?: string
}

export interface Cart {
  id: number
  items: CartItem[]
  itemCount: number
  subtotal: number
}

export interface CartItem {
  id: number
  productId: number
  productName: string
  productSlug: string
  productImage?: string
  productPrice: number
  productStock: number
  quantity: number
  subtotal: number
}

export interface CheckoutSummary {
  subtotal: number
  discount: number
  shippingCost: number
  total: number
  couponCode: string | null
}

export interface CheckoutResponse {
  orderId: number
  orderNumber: string
  provider: string
  preferenceId: string | null
  checkoutUrl: string | null
  amount: number
  currency: string
  paymentStatus: string
}

export interface PaymentStatus {
  orderId: number
  orderNumber: string
  orderStatus: string
  paymentStatus: string
  provider: string | null
  paymentMethod: string | null
  amount: number | null
  currency: string | null
}

export interface Order {
  id: number
  orderNumber: string
  status: OrderStatus
  paymentStatus: string
  subtotal: number
  discount: number
  shippingCost: number
  total: number
  shippingAddress: string
  shippingCity: string
  shippingDepartment?: string
  recipientName?: string
  phone?: string
  paymentMethod?: string
  couponCode?: string
  trackingNumber?: string
  customerName?: string
  customerEmail?: string
  guestEmail?: string
  guestToken?: string
  items: OrderItem[]
  createdAt: string
  updatedAt?: string
}

export interface OrderItem {
  id: number
  productId?: number
  name: string
  price: number
  quantity: number
  subtotal: number
  image?: string
  sku?: string
}

export type OrderStatus = 'PENDING' | 'PAID' | 'PREPARING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

export interface Review {
  id: number
  productId: number
  productName?: string
  userId: number
  userName: string
  rating: number
  title?: string
  comment?: string
  status: string
  createdAt: string
}

export interface Address {
  id: number
  firstName?: string
  lastName?: string
  phone?: string
  addressLine1: string
  addressLine2?: string
  city: string
  department?: string
  postalCode?: string
  addressType: 'SHIPPING' | 'BILLING'
  isDefault: boolean
}

export interface SupportTicket {
  id: number
  ticketNumber: string
  subject: string
  message: string
  category: string
  status: string
  customerName?: string
  customerEmail?: string
  responses?: TicketResponse[]
  createdAt: string
  updatedAt?: string
}

export interface TicketResponse {
  id: number
  message: string
  authorName: string
  authorRole: string
  staff: boolean
  createdAt: string
}

export interface SocialLink {
  id: number
  name: string
  url: string
  icon?: string
  active: boolean
  sortOrder: number
}

export interface Coupon {
  id: number
  code: string
  description?: string
  discountType: string
  discountValue: number
  minAmount?: number
  maxUses?: number
  usesCount: number
  validFrom?: string
  validUntil?: string
  active: boolean
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface AuthResponse {
  token: string
  refreshToken?: string
  type?: string
  id: number
  email: string
  fullName: string
  role: string
}

export interface AiProductSuggestion {
  id: number
  name: string
  slug?: string
  price: number
  image?: string | null
  material?: string | null
  available: boolean
  reason?: string
}

export interface AiConversationTurn {
  role: 'user' | 'assistant'
  content: string
}

export interface AiAssistantResponse {
  message: string
  intent?: string
  products: AiProductSuggestion[]
  followUpQuestion?: string | null
  suggestions?: string[]
}

export type AiAdminActionType = 'UPDATE_ORDER_STATUS' | 'TOGGLE_USER_ACTIVE' | 'MODERATE_REVIEW'

export interface AiAdminAction {
  type: AiAdminActionType
  summary?: string
  parameters: Record<string, string>
}

export interface AiAdminResponse {
  message: string
  suggestedPrompts: string[]
  action?: AiAdminAction | null
  generatedBy: 'AI' | 'RULES'
}

export interface GiftFinderRequest {
  recipient?: string
  occasion?: string
  style?: string
  material?: string
  budgetMin?: number
  budgetMax?: number
}

export interface GiftRecommendation {
  id: number
  name: string
  slug: string
  price: number
  image?: string | null
  material?: string | null
  materialNames?: string[]
  categoryName?: string
  available: boolean
  relevance?: number | null
  explanation?: string
}

export interface GiftFinderResponse {
  recommendations: GiftRecommendation[]
}

export interface DashboardStats {
  totalOrders: number
  totalRevenue: number
  todayRevenue?: number
  monthRevenue?: number
  totalProducts: number
  totalUsers: number
  pendingOrders: number
  totalCustomers?: number
  lowStockProducts?: number
  outOfStockProducts?: number
  topSellingProducts?: Array<{ id: number; name: string; soldCount: number; price?: number; imageUrl?: string | null }>
  recentOrders?: Array<{ id: number; orderNumber: string; total: number; status: string }>
  ordersByStatus?: Record<string, number>
}