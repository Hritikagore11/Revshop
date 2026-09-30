import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { AppNotification, CartItem, Category, Order, OrderItem, Page, Payment, Product, ProductPayload, Profile, Review } from './models';

const s = environment.svc;

/** One typed method per backend endpoint. No component talks to HttpClient directly. */
@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);

  // user-service
  login(email: string, password: string) {
    const body = new HttpParams().set('email', email).set('password', password).toString();
    return this.http.post(`${s.user}/users/login`, body, { responseType: 'text', headers: { 'Content-Type': 'application/x-www-form-urlencoded' } });
  }
  register(u: { name: string; email: string; password: string; phone: string; role: string }) { return this.http.post(`${s.user}/users/register`, u); }
  profile() { return this.http.get<Profile>(`${s.user}/users/profile`); }
  updateProfile(p: { name: string; phone: string; address: string }) { return this.http.put<Profile>(`${s.user}/users/profile`, p); }

  // product-service
  products() { return this.http.get<Product[]>(`${s.product}/api/products`); }
  productsPage(page: number, size: number) { return this.http.get<Page<Product>>(`${s.product}/api/products/paged`, { params: { page, size } }); }
  search(keyword: string) { return this.http.get<Product[]>(`${s.product}/api/products/search`, { params: { keyword } }); }
  product(id: number) { return this.http.get<Product>(`${s.product}/api/products/${id}`); }
  createProduct(p: ProductPayload) { return this.http.post<Product>(`${s.product}/api/products`, p); }
  updateProduct(id: number, p: ProductPayload) { return this.http.put<Product>(`${s.product}/api/products/${id}`, p); }
  deleteProduct(id: number) { return this.http.delete(`${s.product}/api/products/${id}`, { responseType: 'text' }); }
  categories() { return this.http.get<Category[]>(`${s.product}/api/categories`); }
  createCategory(name: string) { return this.http.post<Category>(`${s.product}/api/categories`, { name }); }
  reviews(id: number) { return this.http.get<Review[]>(`${s.product}/api/products/${id}/reviews`); }
  addReview(id: number, r: { rating: number; comment: string }) { return this.http.post<Review>(`${s.product}/api/products/${id}/reviews`, r); }
  updateReview(id: number, r: { rating: number; comment: string }) { return this.http.put<Review>(`${s.product}/api/products/reviews/${id}`, r); }
  deleteReview(id: number) { return this.http.delete(`${s.product}/api/products/reviews/${id}`, { responseType: 'text' }); }

  // cart-service
  cart() { return this.http.get<CartItem[]>(`${s.cart}/cart`); }
  addToCart(productId: number, quantity: number) { return this.http.post<CartItem>(`${s.cart}/cart/items`, null, { params: { productId, quantity } }); }
  setQuantity(itemId: number, quantity: number) { return this.http.put<CartItem>(`${s.cart}/cart/items/${itemId}`, null, { params: { quantity } }); }
  removeItem(itemId: number) { return this.http.delete(`${s.cart}/cart/items/${itemId}`, { responseType: 'text' }); }
  clearCart() { return this.http.delete(`${s.cart}/cart/clear`, { responseType: 'text' }); }

  // order-service + payment-service
  checkout(paymentMethod: string) { return this.http.post<Order>(`${s.order}/orders/checkout`, { paymentMethod }); }
  orders() { return this.http.get<Order[]>(`${s.order}/orders/history`); }
  orderItems(id: number) { return this.http.get<OrderItem[]>(`${s.order}/orders/${id}/items`); }
  setStatus(id: number, status: string) { return this.http.put<Order>(`${s.order}/orders/${id}/status`, null, { params: { status } }); }
  payment(orderId: number) { return this.http.get<Payment>(`${s.payment}/payments/order/${orderId}`); }

  // notification-service
  notifications(uid: number) { return this.http.get<AppNotification[]>(`${s.notif}/notifications/user/${uid}`); }
  unread(uid: number) { return this.http.get<AppNotification[]>(`${s.notif}/notifications/user/${uid}/unread`); }
  lowStock(uid: number) { return this.http.get<AppNotification[]>(`${s.notif}/notifications/user/${uid}/type/LOW_STOCK`); }
  markRead(id: number) { return this.http.put(`${s.notif}/notifications/${id}/read`, null, { responseType: 'text' }); }
  readAll(uid: number) { return this.http.put(`${s.notif}/notifications/user/${uid}/read-all`, null, { responseType: 'text' }); }
}
