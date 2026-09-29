// Zero-dependency dev server. Serves this folder and proxies /svc/<name>/* to your backend,
// so the browser never makes a cross-origin call (none of your services enable CORS).
//
//   node dev-server.js                                  -> talks to each service directly (8081-8086)
//   GATEWAY=http://127.0.0.1:9000 node dev-server.js    -> sends everything through the API gateway
//   PORT=4000 node dev-server.js                        -> change the UI port (default 3000)
const http = require('http'), fs = require('fs'), path = require('path');
const PORTS = { user: 8081, product: 8082, cart: 8083, order: 8084, payment: 8085, notification: 8086 };
const HOST = '127.0.0.1'; // not "localhost": Node 18+ may resolve that to ::1 while Spring listens on IPv4 only
const GATEWAY = process.env.GATEWAY ? new URL(process.env.GATEWAY) : null;
const TYPES = { '.html': 'text/html; charset=utf-8', '.css': 'text/css', '.js': 'text/javascript', '.svg': 'image/svg+xml', '.ico': 'image/x-icon' };

function proxy(req, res, name, upPath) {
  const target = GATEWAY ? { host: GATEWAY.hostname, port: GATEWAY.port || 80 } : { host: HOST, port: PORTS[name] };
  const headers = { ...req.headers, host: `${target.host}:${target.port}` };
  delete headers.origin; delete headers.referer; // avoid Spring's own CORS rejection of "foreign" origins
  const up = http.request({ ...target, path: upPath, method: req.method, headers }, r => { res.writeHead(r.statusCode, r.headers); r.pipe(res); });
  up.on('error', err => {
    console.error(`[proxy] ${name} service unreachable at ${target.host}:${target.port} (${err.code})`);
    res.writeHead(502, { 'Content-Type': 'application/json', 'X-Proxy-Error': name });
    res.end(JSON.stringify({ error: 'unreachable', service: name }));
  });
  req.pipe(up);
}

http.createServer((req, res) => {
  const m = req.url.match(/^\/svc\/(\w+)(\/.*)?$/);
  if (m && PORTS[m[1]]) return proxy(req, res, m[1], m[2] || '/');
  const rel = decodeURIComponent(req.url.split('?')[0]);
  const f = path.join(__dirname, rel === '/' ? 'index.html' : rel);
  if (!f.startsWith(__dirname)) { res.writeHead(403); return res.end(); }
  fs.readFile(f, (e, d) => {
    if (e) { res.writeHead(404); return res.end('Not found'); }
    res.writeHead(200, { 'Content-Type': TYPES[path.extname(f)] || 'application/octet-stream', 'Cache-Control': 'no-cache' });
    res.end(d);
  });
}).listen(+process.env.PORT || 3000, () => console.log(`RevShop UI on http://localhost:${process.env.PORT || 3000}` + (GATEWAY ? ` (via gateway ${GATEWAY.origin})` : ' (direct to services)')));
