// Development: the dev server proxies /svc/<service> to your Spring services (see proxy.conf.js).
export const environment = {
  production: false,
  svc: { user: '/svc/user', product: '/svc/product', cart: '/svc/cart', order: '/svc/order', payment: '/svc/payment', notif: '/svc/notification' },
};
