import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards';

const T = (t: string) => `${t} · RevShop`;
export const routes: Routes = [
  { path: '', pathMatch: 'full', title: T('Home'), canActivate: [authGuard], loadComponent: () => import('./pages/home').then(m => m.Home) },
  { path: 'products', title: T('Products'), canActivate: [authGuard], loadComponent: () => import('./pages/catalog').then(m => m.Catalog) },
  { path: 'product/:id', title: T('Product'), canActivate: [authGuard], loadComponent: () => import('./pages/product-detail').then(m => m.ProductDetail) },
  { path: 'cart', title: T('Cart'), canActivate: [authGuard, roleGuard('BUYER')], loadComponent: () => import('./pages/cart').then(m => m.Cart) },
  { path: 'checkout', title: T('Checkout'), canActivate: [authGuard, roleGuard('BUYER')], loadComponent: () => import('./pages/checkout').then(m => m.Checkout) },
  { path: 'orders', title: T('Orders'), canActivate: [authGuard, roleGuard('BUYER')], loadComponent: () => import('./pages/orders').then(m => m.Orders) },
  { path: 'notifications', title: T('Notifications'), canActivate: [authGuard], loadComponent: () => import('./pages/notifications').then(m => m.Notifications) },
  { path: 'seller', title: T('Seller dashboard'), canActivate: [authGuard, roleGuard('SELLER')], loadComponent: () => import('./pages/seller').then(m => m.Seller) },
  { path: 'login', title: T('Log in'), canActivate: [guestGuard], loadComponent: () => import('./pages/auth').then(m => m.Login) },
  { path: 'register', title: T('Create account'), canActivate: [guestGuard], loadComponent: () => import('./pages/auth').then(m => m.Register) },
  { path: '**', title: T('Not found'), loadComponent: () => import('./pages/not-found').then(m => m.NotFound) },
];
