// Production: everything goes through the API gateway, which nginx exposes at /gw (see nginx.conf).
// Change '/gw' if your gateway is published elsewhere (e.g. 'https://api.example.com').
const gw = '/gw';
export const environment = {
  production: true,
  svc: { user: gw, product: gw, cart: gw, order: gw, payment: gw, notif: gw },
};
