export type Role = 'BUYER' | 'SELLER';
export type OrderStatus = 'PLACED' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'PAYMENT_FAILED';
export interface Category { id: number; name: string }
export interface Product {
  id: number; name: string; description?: string | null; imageUrl?: string | null; price: number;
  discount?: number | null; quantity: number; lowStockThreshold?: number | null; sellerId: number; category?: Category | null;
}
export interface Page<T> { content: T[]; totalPages: number; totalElements: number; number: number }
export interface Review { id: number; buyerId: number; rating: number; comment?: string | null }
export interface CartItem { id: number; productId: number; quantity: number }
export interface Order { id: number; userId: number; totalAmount: number; status: OrderStatus; createdAt: string }
export interface OrderItem { id: number; productId: number; quantity: number; price: number }
export interface Payment { paymentMethod: string; status: string; transactionId: string }
export interface AppNotification { id: number; message: string; type?: string; isRead?: boolean; read?: boolean; createdAt: string }
export interface Profile { id?: number; name: string; email: string; phone?: string | null; address?: string | null; role: Role }
export interface Claims { sub: string; userId: number; role: Role; exp: number }
export interface ProductPayload {
  name: string; description: string; imageUrl: string | null; price: number; discount: number;
  quantity: number; lowStockThreshold: number; category: { id: number } | null;
}
