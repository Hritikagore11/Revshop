// Dev-only proxy: the browser talks to http://localhost:4200/svc/<service>/..., Angular forwards it to Spring.
// No CORS setup needed on the backend.
//   npm start                                   -> straight to each service (8081-8086)
//   GATEWAY=http://127.0.0.1:9000 npm start     -> everything through the API gateway
const PORTS = { user: 8081, product: 8082, cart: 8083, order: 8084, payment: 8085, notification: 8086 };
const base = { changeOrigin: true, secure: false, on: { proxyReq: p => { p.removeHeader('origin'); p.removeHeader('referer'); } } };
const gw = process.env.GATEWAY;

module.exports = gw
  ? { '/svc': { ...base, target: gw, pathRewrite: { '^/svc/\\w+': '' } } }
  : Object.fromEntries(Object.entries(PORTS).map(([name, port]) => [`/svc/${name}`, { ...base, target: `http://127.0.0.1:${port}`, pathRewrite: { [`^/svc/${name}`]: '' } }]));
