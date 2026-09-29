'use strict';
/* ---------- config ---------- */
// Default paths go through dev-server.js (no CORS needed). To call services directly instead, use
// e.g. user:'http://localhost:8081' and enable CORS on each Spring service.
const CFG = {
  api: { user: '/svc/user', product: '/svc/product', cart: '/svc/cart', order: '/svc/order',
         payment: '/svc/payment', notif: '/svc/notification' },
  delivery: 0 // backend adds no delivery fee; keep 0 unless order-service does
};

/* ---------- helpers ---------- */
const $ = (s, r = document) => r.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => '₹' + Number(n || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 });
const fin = p => p.price * (1 - (p.discount || 0) / 100);
const fdate = t => new Date(t).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
const ordId = o => `ORD-${new Date(o.createdAt).getFullYear()}-${String(o.id).padStart(5, '0')}`;
const ago = t => { const m = (Date.now() - new Date(t)) / 6e4; return m < 1 ? 'Just now' : m < 60 ? `${m | 0} min ago` : m < 1440 ? `${m / 60 | 0} hours ago` : m < 2880 ? 'Yesterday' : fdate(t); };
const V = $('#view'), view = h => { V.innerHTML = h; };
const stars = n => '★'.repeat(Math.round(n)) + '☆'.repeat(5 - Math.round(n));
const isLow = p => p.quantity > 0 && p.quantity <= (p.lowStockThreshold ?? 5);
const stock = p => p.quantity <= 0 ? ['bad', 'Out of stock'] : isLow(p) ? ['warn', p.quantity + ' left'] : ['ok', 'In stock'];
const STAT = { PLACED: ['neutral', 'Placed'], CONFIRMED: ['warn', 'Confirmed'], SHIPPED: ['ok', 'Shipped'],
  DELIVERED: ['ok', 'Delivered'], CANCELLED: ['bad', 'Cancelled'], PAYMENT_FAILED: ['bad', 'Payment failed'] };
const sbadge = s => { const [k, t] = STAT[s] || ['neutral', s]; return `<span class="badge ${k}">${t}</span>`; };
const priceHtml = p => `<span class="price">${money(fin(p))}</span>${p.discount > 0 ? `<s class="was">${money(p.price)}</s>` : ''}`;
const fld = (n, l, type = 'text', v = '', x = '') => `<div class="field"><label for="${n}">${l}</label>${type === 'textarea'
  ? `<textarea id="${n}" name="${n}" rows="3" ${x}>${esc(v)}</textarea>` : `<input id="${n}" name="${n}" type="${type}" value="${esc(v)}" ${x}>`}<span class="help"></span></div>`;
const empty = (t, href, label) => `<div class="empty"><p>${t}</p>${href ? `<a class="btn btn-primary" href="${href}">${label}</a>` : ''}</div>`;
const grid = (ps, c = 'g3') => ps.length ? `<div class="grid ${c}">${ps.map(card).join('')}</div>` : empty('No products match — clear the filters or try another search.');
const thumb = p => { const f = `<span class="img-fallback" aria-hidden="true">${esc((p?.name || '?')[0])}</span>`;
  return p?.imageUrl ? `<img src="${esc(p.imageUrl)}" alt="" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">${f}` : f; };
const card = p => { const [k, t] = stock(p);
  return `<a class="pc" href="#/product/${p.id}"><div class="img">${thumb(p)}</div><div class="body"><div class="name">${esc(p.name)}</div><div class="pc-row"><span>${priceHtml(p)}</span><span class="badge ${k}">${t}</span></div></div></a>`; };
const summary = (n, sub, btn) => { const d = CFG.delivery;
  return `<aside class="summary"><div class="line"><span>${n} item${n === 1 ? '' : 's'}</span><span class="price">${money(sub)}</span></div><div class="line"><span>Delivery</span><span class="price">${d ? money(d) : 'Free'}</span></div><div class="line total"><span>Total</span><span class="price">${money(sub + d)}</span></div>${btn}</aside>`; };

/* ---------- session + api ---------- */
const S = { token: localStorage.getItem('rs_token'), name: null, profile: null, flash: null };
let U = null;
function claims() { try { const c = JSON.parse(atob(S.token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))); return c.exp * 1000 > Date.now() ? c : null; } catch { return null; } }
function logout(m) { localStorage.removeItem('rs_token'); S.token = S.name = S.profile = null; S.flash = m || null; U = null; location.hash === '#/login' ? route() : (location.hash = '#/login'); }
class Err extends Error { constructor(s, m, svc) { super(m); this.status = s; this.svc = svc; } }
const SVC = { user: 'user', product: 'product', cart: 'cart', order: 'order', payment: 'payment', notif: 'notification' };
async function api(svc, path, o = {}) {
  const h = {}; if (S.token) h.Authorization = 'Bearer ' + S.token;
  let body; if (o.json) { h['Content-Type'] = 'application/json'; body = JSON.stringify(o.json); } else if (o.form) body = new URLSearchParams(o.form);
  let r; try { r = await fetch(CFG.api[svc] + path, { method: o.method || 'GET', headers: h, body }); } catch { throw new Err(0, '', svc); }
  if ([502, 503, 504].includes(r.status)) throw new Err(0, '', svc);
  const t = await r.text(); let d = t; try { d = JSON.parse(t); } catch { /* plain text (e.g. JWT) */ }
  if (!r.ok) { if ((r.status === 401 || r.status === 403) && S.token && !claims()) logout('Session expired. Log in again to continue.'); throw new Err(r.status, (d && d.message) || (typeof d === 'string' ? d : '')); }
  return d;
}
const MAP = [[/already registered/i, 'That email is already registered. Log in instead.'], [/insufficient stock|out of stock/i, 'Not enough stock available. Lower the quantity and try again.'],
  [/already reviewed/i, 'You have already reviewed this product. Edit your review instead.'], [/cart is empty/i, 'Your cart is empty. Add a product first.'],
  [/payment/i, 'Payment failed. Check your details and try again.'], [/password must/i, 'Password must be at least 6 characters.'], [/cannot be cancelled/i, 'This order can no longer be cancelled.']];
function msg(e, fb) {
  if (e.status === 0) return `Can’t reach the ${SVC[e.svc] || 'server'} service. Make sure it is running, then try again.`;
  for (const [r, m] of MAP) if (r.test(e.message)) return m;
  if (e.status === 403) return 'You don’t have permission to do that.';
  if (e.status === 404) return 'We couldn’t find that.';
  return fb || 'Something went wrong — try again.';
}
const toast = (t, k = 'ok') => { $('#toast').innerHTML = `<div class="alert ${k}">${esc(t)}</div>`; clearTimeout(toast.t); toast.t = setTimeout(() => $('#toast').innerHTML = '', 4000); };
const fail = (e, fb) => toast(msg(e, fb), 'error');
function modal(h) { const d = $('#dlg'); d.innerHTML = h; d.onclick = e => { if (e.target === d || e.target.closest('[data-close]')) d.close(); }; d.showModal(); return d; }
const confirmBox = (text, label) => new Promise(res => { const d = modal(`<h3>${esc(text)}</h3><div class="row end"><button class="btn btn-secondary" data-close>Keep</button><button class="btn btn-danger" id="yes">${label}</button></div>`);
  $('#yes', d).onclick = () => { res(true); d.close(); }; d.addEventListener('close', () => res(false), { once: true }); });
const products = () => api('product', '/api/products');
const productsPage = (page = 0, size = 12) => api('product', `/api/products/paged?page=${page}&size=${size}`);
const pmap = async () => new Map((await products()).map(p => [p.id, p]));
let CATS = null; const cats = async f => (!CATS || f) ? (CATS = await api('product', '/api/categories')) : CATS;
const bad = (f, n, t) => { const el = f.elements[n]?.closest('.field'); if (el) { el.classList.add('err'); $('.help', el).textContent = t; } return true; };
const clr = f => f.querySelectorAll('.field.err').forEach(e => { e.classList.remove('err'); $('.help', e).textContent = ''; });
const ferr = (f, t) => { let b = $('.ferr', f); if (!b) { b = document.createElement('div'); b.className = 'ferr'; f.prepend(b); } b.innerHTML = t ? `<div class="alert error">${esc(t)}</div>` : ''; };

/* ---------- nav ---------- */
function nav(path) {
  const b = U?.role === 'BUYER';
  const L = U ? [['/', 'Home'], ['/products', 'Products'], ...(b ? [['/cart', 'Cart'], ['/orders', 'Orders']] : [['/seller', 'Seller dashboard']]), ['/notifications', 'Notifications']] : [];
  $('#nav').innerHTML = L.map(([h, t]) => `<a href="#${h}"${path === h ? ' aria-current="page"' : ''}>${t}${h === '/cart' ? '<span class="count" id="cc"></span>' : ''}${h === '/notifications' ? '<span class="pip" id="nd" hidden></span>' : ''}</a>`).join('');
  $('#user').innerHTML = U ? `<span>${esc(S.name || U.sub)}</span><span class="badge ${b ? 'buyer' : 'seller'}">${b ? 'Buyer' : 'Seller'}</span><button class="btn btn-secondary btn-sm" data-act="out">Log out</button>` : '';
  if (U) counts();
}
async function counts() {
  try { if (U.role === 'BUYER') { const c = await api('cart', '/cart'); $('#cc').textContent = c.length || ''; } } catch { /* non-critical */ }
  try { const n = await api('notif', `/notifications/user/${U.userId}/unread`); $('#nd').hidden = !n.length; } catch { /* non-critical */ }
}

/* ---------- pages ---------- */
function login() {
  view(`<div class="auth"><h1>Log in</h1>${S.flash ? `<div class="alert error">${esc(S.flash)}</div>` : ''}<form data-form="login" novalidate><div class="ferr"></div>${fld('email', 'Email', 'email', '', 'autocomplete="username"')}${fld('password', 'Password', 'password', '', 'autocomplete="current-password"')}<button class="btn btn-primary btn-block">Log in</button></form><p class="muted">New to RevShop? <a href="#/register">Create an account</a></p></div>`);
  S.flash = null;
}
function register() {
  view(`<div class="auth"><h1>Create account</h1><form data-form="register" novalidate><div class="ferr"></div>${fld('name', 'Full name')}${fld('email', 'Email', 'email')}${fld('password', 'Password', 'password', '', 'autocomplete="new-password"')}${fld('phone', 'Phone', 'tel')}<div class="field"><label for="role">I want to</label><select id="role" name="role"><option value="BUYER">Buy products</option><option value="SELLER">Sell products</option></select></div><button class="btn btn-primary btn-block">Create account</button></form><p class="muted">Already registered? <a href="#/login">Log in</a></p></div>`);
}
async function home() {
  const [pg, cs] = await Promise.all([productsPage(0, 8), cats(true)]); const ps = pg.content || [];
  view(`<div class="hero"><h1>Everything you need, from sellers you trust</h1><p>Browse products across categories${U.role === 'BUYER' ? ', add to cart and check out in a few clicks' : ' and manage your listings from the seller dashboard'}.</p><a class="btn btn-primary" href="#/products">Browse products</a></div>
<div class="chips"><a class="chip active" href="#/products">All</a>${cs.map(c => `<a class="chip" href="#/products?cat=${c.id}">${esc(c.name)}</a>`).join('')}</div>
<div class="section-h"><h2>Featured products</h2><a href="#/products">View all ›</a></div>${grid(ps.slice(0, 8), 'g4')}`);
}
const PR = { under: p => fin(p) < 500, mid: p => fin(p) >= 500 && fin(p) <= 2000, over: p => fin(p) > 2000 };
const PAGE = 12;
async function catalog(q) {
  const kw = q.get('q') || '', sel = new Set(q.getAll('cat')), pr = q.get('pr') || '', page = Math.max(0, +(q.get('page') || 0));
  const filtered = !!(kw || sel.size || pr);
  let list, totalPages = 1, total;
  const cs = await cats(true);
  if (!filtered) { const pg = await productsPage(page, PAGE); list = pg.content || []; totalPages = pg.totalPages || 1; total = pg.totalElements ?? list.length; }
  else {
    const all = (kw ? await api('product', '/api/products/search?keyword=' + encodeURIComponent(kw)) : await products())
      .filter(p => (!sel.size || sel.has(String(p.category?.id))) && (!pr || PR[pr](p)));
    total = all.length; totalPages = Math.max(1, Math.ceil(total / PAGE)); list = all.slice(page * PAGE, page * PAGE + PAGE);
  }
  const href = n => { const u = new URLSearchParams(q); u.set('page', n); return '#/products?' + u; };
  const pageLinks = totalPages > 1 ? `<nav class="pagination" aria-label="Pages">${page > 0 ? `<a class="btn btn-secondary btn-sm" href="${href(page - 1)}">‹ Previous</a>` : '<span></span>'}<span class="meta">Page ${page + 1} of ${totalPages} · ${total} products</span>${page + 1 < totalPages ? `<a class="btn btn-secondary btn-sm" href="${href(page + 1)}">Next ›</a>` : '<span></span>'}</nav>` : '';
  view(`<form data-form="filter" class="plist"><div class="filters"><h4>Category</h4>${cs.map(c => `<label><input type="checkbox" name="cat" value="${c.id}"${sel.has(String(c.id)) ? ' checked' : ''}> ${esc(c.name)}</label>`).join('') || '<span class="meta">No categories yet</span>'}
<h4>Price</h4>${[['', 'Any price'], ['under', 'Under ₹500'], ['mid', '₹500 – ₹2,000'], ['over', 'Over ₹2,000']].map(([v, t]) => `<label><input type="radio" name="pr" value="${v}"${pr === v ? ' checked' : ''}> ${t}</label>`).join('')}</div>
<div><div class="searchbar"><input name="q" type="search" placeholder="Search products…" value="${esc(kw)}" aria-label="Search products"><button class="btn btn-secondary">Search</button></div>${grid(list)}${pageLinks}</div></form>`);
}
async function detail(q, id) {
  const [p, rv] = await Promise.all([api('product', '/api/products/' + id), api('product', `/api/products/${id}/reviews`)]);
  const [k, t] = stock(p), buyer = U.role === 'BUYER', mine = rv.find(r => r.buyerId === U.userId);
  const avg = rv.length ? rv.reduce((s, r) => s + r.rating, 0) / rv.length : 0;
  view(`<div class="detail"><div class="img detail-image">${p.imageUrl ? `<img src="${esc(p.imageUrl)}" alt="${esc(p.name)}" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'"><span class="img-fallback" aria-hidden="true">${esc((p.name || '?')[0])}</span>` : `<span class="img-fallback" aria-hidden="true">${esc((p.name || '?')[0])}</span>`}</div><div>
<h1 style="font-size:24px;margin-bottom:6px">${esc(p.name)}</h1>
<div class="stars" aria-label="${avg.toFixed(1)} out of 5">${rv.length ? stars(avg) : ''} <span class="meta">${rv.length ? `(${rv.length} review${rv.length > 1 ? 's' : ''})` : 'No reviews yet'}</span></div>
<div style="font-size:22px;margin:14px 0">${priceHtml(p)}${p.discount > 0 ? ` <span class="badge ok">${p.discount}% off</span>` : ''}</div>
<span class="badge ${k}">${p.quantity > 0 ? t + (k === 'ok' ? ` — ${p.quantity} available` : '') : t}</span>
<p class="muted" style="max-width:50ch;margin:16px 0">${esc(p.description || 'No description provided.')}</p>
${buyer ? `<div class="row" style="gap:16px;margin-bottom:20px"><div class="qty"><button data-act="qty" data-d="-1" aria-label="Decrease">−</button><span id="q" data-max="${p.quantity}">1</span><button data-act="qty" data-d="1" aria-label="Increase">+</button></div>${p.quantity > 0 ? `<button class="btn btn-primary" data-act="add" data-id="${p.id}">Add to cart</button>` : '<button class="btn btn-disabled" disabled>Out of stock</button>'}</div>` : ''}
<h3>Reviews</h3>${rv.map(r => `<div class="review"><div class="row"><span><b>${r.buyerId === U.userId ? 'You' : 'Buyer #' + r.buyerId}</b> · <span class="stars">${stars(r.rating)}</span></span>${r.buyerId === U.userId ? `<button class="btn btn-danger btn-sm" data-act="delrev" data-id="${r.id}" data-pid="${p.id}">Delete</button>` : ''}</div><div class="muted">${esc(r.comment || '')}</div></div>`).join('') || '<p class="meta">No reviews yet — be the first to share how it went.</p>'}
${buyer ? `<form data-form="review" data-pid="${p.id}" data-rid="${mine?.id || ''}" novalidate style="margin-top:16px"><div class="ferr"></div><div class="field"><label for="rating">Your rating</label><select id="rating" name="rating">${[5, 4, 3, 2, 1].map(n => `<option value="${n}"${mine?.rating === n ? ' selected' : ''}>${n} — ${stars(n)}</option>`).join('')}</select></div>${fld('comment', 'Comment', 'textarea', mine?.comment || '')}<button class="btn btn-secondary">${mine ? 'Update review' : 'Post review'}</button></form>` : ''}
</div></div>`);
}
async function cart() {
  const [items, pm] = await Promise.all([api('cart', '/cart'), pmap()]);
  if (!items.length) return view(`<h1>Your cart</h1>${empty('Your cart is empty — browse products to get started.', '#/products', 'Browse products')}`);
  const blocked = items.some(i => { const p = pm.get(i.productId); return !p || p.quantity < i.quantity; });
  const sub = items.reduce((s, i) => s + (pm.get(i.productId) ? fin(pm.get(i.productId)) * i.quantity : 0), 0);
  view(`<h1>Your cart</h1>${blocked ? '<div class="alert warn">Some items are no longer available in the quantity you chose. Update or remove them to continue.</div>' : ''}<div class="cc"><div>
${items.map(i => { const p = pm.get(i.productId); return `<div class="cart-item"><div class="img">${thumb(p)}</div><div><div style="font-weight:600;font-size:14px">${p ? `<a href="#/product/${p.id}" style="color:inherit">${esc(p.name)}</a>` : 'Unavailable product #' + i.productId}</div><div class="meta">${p ? money(fin(p)) + ' each' : 'Remove this item to continue'}</div></div>
<div class="qty"><button data-act="cq" data-id="${i.id}" data-q="${i.quantity - 1}" aria-label="Decrease"${i.quantity <= 1 ? ' disabled' : ''}>−</button><span>${i.quantity}</span><button data-act="cq" data-id="${i.id}" data-q="${i.quantity + 1}" aria-label="Increase"${!p || i.quantity >= p.quantity ? ' disabled' : ''}>+</button></div>
<span class="price">${p ? money(fin(p) * i.quantity) : '—'}</span><button class="btn btn-danger btn-sm" data-act="crm" data-id="${i.id}">Remove</button></div>`; }).join('')}
<div style="margin-top:16px"><button class="btn btn-secondary btn-sm" data-act="cclr">Clear cart</button></div></div>
${summary(items.reduce((s, i) => s + i.quantity, 0), sub, blocked ? '<button class="btn btn-disabled btn-block" style="margin-top:14px" disabled>Checkout</button>' : '<a class="btn btn-primary btn-block" style="margin-top:14px" href="#/checkout">Checkout</a>')}</div>`);
}
async function checkout() {
  const [items, pm, me] = await Promise.all([api('cart', '/cart'), pmap(), api('user', '/users/profile')]);
  if (!items.length) { location.hash = '#/cart'; return; }
  S.profile = me;
  const sub = items.reduce((s, i) => s + (pm.get(i.productId) ? fin(pm.get(i.productId)) * i.quantity : 0), 0);
  view(`<h1>Checkout</h1><div class="cc"><form id="ship" data-form="ship" novalidate><div class="ferr"></div><h3 style="margin-top:0">Shipping address</h3>${fld('name', 'Full name', 'text', me.name || '')}${fld('address', 'Address', 'text', me.address || '')}
<div class="split">${fld('city', 'City')}${fld('pin', 'PIN code', 'text', '', 'inputmode="numeric" maxlength="6"')}</div>
<label class="row meta" style="margin-bottom:8px"><input type="checkbox" name="save" checked> Save this address to my profile</label>
<h3>Payment method</h3><label class="pay-opt"><input type="radio" name="pay" value="COD" checked> Cash on delivery</label><label class="pay-opt"><input type="radio" name="pay" value="CARD"> Credit / debit card (simulated)</label></form>
${summary(items.reduce((s, i) => s + i.quantity, 0), sub, '<button class="btn btn-primary btn-block" style="margin-top:14px" form="ship">Place order</button>')}</div>`);
}
let ORD = new Map();
async function orders() {
  const os = (await api('order', '/orders/history')).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)); ORD = new Map(os.map(o => [o.id, o]));
  view(`<h1>Your orders</h1>${os.length ? os.map(o => `<div class="order-row"><div><span class="mono">${ordId(o)}</span><div class="meta">Placed ${fdate(o.createdAt)}</div></div><span class="price">${money(o.totalAmount)}</span>${sbadge(o.status)}<button class="btn btn-secondary btn-sm" data-act="ord" data-id="${o.id}">View details</button></div>`).join('') : empty('No orders yet — browse products to get started.', '#/products', 'Browse products')}`);
}
async function notifs() {
  let ns;
  if (U.role === 'BUYER') ns = await api('notif', '/notifications/user/' + U.userId);
  else { const [a, b] = await Promise.all([api('notif', `/notifications/user/${U.userId}/unread`), api('notif', `/notifications/user/${U.userId}/type/LOW_STOCK`)]); ns = [...new Map([...a, ...b].map(n => [n.id, n])).values()]; }
  ns.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  const rd = n => n.isRead ?? n.read, unread = ns.some(n => !rd(n));
  view(`<h1>Notifications</h1>${ns.length ? `<div class="row end" style="margin:0 0 8px"><button class="btn btn-secondary btn-sm" data-act="nall"${unread ? '' : ' disabled'}>Mark all as read</button></div>` : ''}
${ns.map(n => { const inner = `<span class="dot${rd(n) ? ' none' : ''}"></span><span><span class="t" style="display:block">${esc(n.message)}</span><span class="m" style="display:block">${ago(n.createdAt)}</span></span>`;
  return !rd(n) && U.role === 'BUYER' ? `<button class="notif" data-act="nread" data-id="${n.id}" title="Mark as read">${inner}</button>` : `<div class="notif">${inner}</div>`; }).join('') || empty('You’re all caught up — order and stock updates will show up here.')}`);
}
let SP = new Map();
async function seller() {
  const [ps] = await Promise.all([products(), cats(true)]); const mine = ps.filter(p => p.sellerId === U.userId); SP = new Map(mine.map(p => [p.id, p]));
  const low = mine.filter(p => p.quantity <= (p.lowStockThreshold ?? 5));
  view(`<h1>Seller dashboard</h1>${low.slice(0, 3).map(p => `<div class="alert warn">${esc(p.name)} is ${p.quantity ? 'low on stock (' + p.quantity + ' left)' : 'out of stock'} — restock soon.</div>`).join('')}${low.length > 3 ? `<div class="alert warn">${low.length - 3} more products need restocking.</div>` : ''}
<div class="section-h"><h2>Your products</h2><button class="btn btn-primary btn-sm" data-act="padd">Add product</button></div>
${mine.length ? `<div class="tbl"><table><thead><tr><th>Product</th><th class="num">Price</th><th class="num">Stock</th><th>Status</th><th></th></tr></thead><tbody>${mine.map(p => { const [k, t] = p.quantity <= 0 ? ['bad', 'Out of stock'] : isLow(p) ? ['warn', 'Low stock'] : ['ok', 'Active'];
  return `<tr><td><a href="#/product/${p.id}">${esc(p.name)}</a></td><td class="num">${money(fin(p))}</td><td class="num">${p.quantity}</td><td><span class="badge ${k}">${t}</span></td><td class="row"><button class="btn btn-secondary btn-sm" data-act="pedit" data-id="${p.id}">Edit</button><button class="btn btn-danger btn-sm" data-act="pdel" data-id="${p.id}">Delete</button></td></tr>`; }).join('')}</tbody></table></div>` : empty('No products yet — add your first product to start selling.')}
<div class="section-h" style="margin-top:32px"><h2>Update an order</h2></div><form class="panel row" data-form="ostat" novalidate style="align-items:flex-end"><div class="field" style="margin:0;width:140px"><label for="oid">Order number</label><input id="oid" name="oid" type="number" min="1" required><span class="help"></span></div><div class="field" style="margin:0;width:180px"><label for="st">New status</label><select id="st" name="st"><option value="CONFIRMED">Confirmed</option><option value="SHIPPED">Shipped</option><option value="DELIVERED">Delivered</option></select></div><button class="btn btn-secondary">Update status</button><span class="meta" style="flex-basis:100%">Statuses move Placed → Confirmed → Shipped → Delivered, one step at a time.</span></form>
<div class="section-h" style="margin-top:32px"><h2>Categories</h2></div><form class="panel row" data-form="cat" novalidate style="align-items:flex-end"><div class="field" style="margin:0;width:240px"><label for="cname">New category</label><input id="cname" name="cname"><span class="help"></span></div><button class="btn btn-secondary">Add category</button></form>`);
}
async function productForm(p) {
  const cs = await cats();
  const d = modal(`<h2>${p ? 'Edit product' : 'Add product'}</h2><form data-form="product" data-id="${p?.id || ''}" novalidate><div class="ferr"></div>${fld('name', 'Name', 'text', p?.name || '')}${fld('description', 'Description', 'textarea', p?.description || '')}${fld('imageUrl', 'Image URL', 'url', p?.imageUrl || '', 'placeholder="https://..."')}
<div class="split">${fld('price', 'Price (₹)', 'number', p?.price ?? '', 'min="0" step="0.01"')}${fld('discount', 'Discount (%)', 'number', p?.discount ?? 0, 'min="0" max="100"')}</div>
<div class="split">${fld('quantity', 'Stock', 'number', p?.quantity ?? '', 'min="0"')}${fld('threshold', 'Low-stock alert at', 'number', p?.lowStockThreshold ?? 5, 'min="0"')}</div>
<div class="field"><label for="category">Category</label><select id="category" name="category"><option value="">No category</option>${cs.map(c => `<option value="${c.id}"${p?.category?.id === c.id ? ' selected' : ''}>${esc(c.name)}</option>`).join('')}</select></div>
<div class="row end"><button type="button" class="btn btn-secondary" data-close>Cancel</button><button class="btn btn-primary">${p ? 'Save changes' : 'Add product'}</button></div></form>`); return d;
}

/* ---------- actions (click) ---------- */
const ACT = {
  out: () => logout(),
  qty: a => { const q = $('#q'), n = Math.min(Math.max(1, +q.textContent + +a.dataset.d), +q.dataset.max || 1); q.textContent = n; },
  add: async a => { try { await api('cart', `/cart/items?productId=${a.dataset.id}&quantity=${$('#q').textContent}`, { method: 'POST' }); toast('Added to cart.'); counts(); } catch (e) { fail(e, 'We couldn’t add this to your cart — try again.'); } },
  cq: async a => { try { await api('cart', `/cart/items/${a.dataset.id}?quantity=${a.dataset.q}`, { method: 'PUT' }); route(); } catch (e) { fail(e, 'We couldn’t update the quantity — try again.'); } },
  crm: async a => { try { await api('cart', '/cart/items/' + a.dataset.id, { method: 'DELETE' }); toast('Item removed from cart.'); route(); } catch (e) { fail(e); } },
  cclr: async () => { if (await confirmBox('Remove every item from your cart?', 'Clear cart')) try { await api('cart', '/cart/clear', { method: 'DELETE' }); toast('Cart cleared.'); route(); } catch (e) { fail(e); } },
  delrev: async a => { if (await confirmBox('Delete your review?', 'Delete review')) try { await api('product', '/api/products/reviews/' + a.dataset.id, { method: 'DELETE' }); toast('Review deleted.'); route(); } catch (e) { fail(e); } },
  ord: async a => {
    const o = ORD.get(+a.dataset.id); modal('<p class="muted">Loading…</p>');
    try {
      const [items, pm, pay] = await Promise.all([api('order', `/orders/${o.id}/items`), pmap(), api('payment', '/payments/order/' + o.id).catch(() => null)]);
      const can = !['DELIVERED', 'CANCELLED', 'PAYMENT_FAILED'].includes(o.status);
      modal(`<h2><span class="mono">${ordId(o)}</span></h2><div class="row" style="margin-bottom:12px">${sbadge(o.status)}<span class="meta">Placed ${fdate(o.createdAt)}</span></div>
<div class="tbl"><table><thead><tr><th>Item</th><th class="num">Qty</th><th class="num">Price</th></tr></thead><tbody>${items.map(i => `<tr><td>${esc(pm.get(i.productId)?.name || 'Product #' + i.productId)}</td><td class="num">${i.quantity}</td><td class="num">${money(i.price * i.quantity)}</td></tr>`).join('')}<tr><td colspan="2"><b>Total</b></td><td class="num"><b>${money(o.totalAmount)}</b></td></tr></tbody></table></div>
${pay ? `<p class="meta">Paid by ${pay.paymentMethod === 'COD' ? 'cash on delivery' : 'card'} · ${esc(pay.status.toLowerCase())}<br>Transaction <span class="mono" style="font-size:12px">${esc(pay.transactionId)}</span></p>` : ''}
<div class="row end"><button class="btn btn-secondary" data-close>Close</button>${can ? `<button class="btn btn-danger" data-act="cancel" data-id="${o.id}">Cancel order</button>` : ''}</div>`);
    } catch (e) { $('#dlg').close(); fail(e, 'We couldn’t load this order — try again.'); }
  },
  cancel: async a => { $('#dlg').close(); if (await confirmBox('Cancel this order? Items go back into stock.', 'Cancel order')) try { await api('order', `/orders/${a.dataset.id}/status?status=CANCELLED`, { method: 'PUT' }); toast('Order cancelled.'); route(); } catch (e) { fail(e, 'We couldn’t cancel this order — try again.'); } },
  nread: async a => { try { await api('notif', `/notifications/${a.dataset.id}/read`, { method: 'PUT' }); route(); } catch (e) { fail(e); } },
  nall: async () => { try { await api('notif', `/notifications/user/${U.userId}/read-all`, { method: 'PUT' }); route(); } catch (e) { fail(e); } },
  padd: () => productForm(null),
  pedit: a => productForm(SP.get(+a.dataset.id)),
  pdel: async a => { if (await confirmBox(`Delete “${SP.get(+a.dataset.id)?.name}”? This can’t be undone.`, 'Delete product')) try { await api('product', '/api/products/' + a.dataset.id, { method: 'DELETE' }); toast('Product deleted.'); route(); } catch (e) { fail(e, 'We couldn’t delete this product — try again.'); } }
};
document.addEventListener('click', e => { const a = e.target.closest('[data-act]'); if (a) ACT[a.dataset.act]?.(a, e); });

/* ---------- forms (submit) ---------- */
async function signIn(email, password) {
  S.token = await api('user', '/users/login', { method: 'POST', form: { email, password } }); localStorage.setItem('rs_token', S.token); U = claims();
  try { S.profile = await api('user', '/users/profile'); S.name = S.profile.name; } catch { /* name falls back to email */ }
  location.hash = U.role === 'SELLER' ? '#/seller' : '#/';
}
const num = v => v === '' ? NaN : +v;
const FORMS = {
  async login(f, d) {
    clr(f); ferr(f, ''); const email = d.get('email').trim(), pw = d.get('password'); let x = false;
    if (!/^\S+@\S+\.\S+$/.test(email)) x = bad(f, 'email', 'Enter a valid email like you@example.com.');
    if (!pw) x = bad(f, 'password', 'Enter your password.'); if (x) return;
    try { await signIn(email, pw); } catch (e) { S.token = null; ferr(f, e.status === 0 ? msg(e) : 'Email or password is incorrect. Check them and try again.'); }
  },
  async register(f, d) {
    clr(f); ferr(f, ''); const email = d.get('email').trim(); let x = false;
    if (!d.get('name').trim()) x = bad(f, 'name', 'Enter your full name.');
    if (!/^\S+@\S+\.\S+$/.test(email)) x = bad(f, 'email', 'Enter a valid email like you@example.com.');
    if (d.get('password').length < 6) x = bad(f, 'password', 'Must be at least 6 characters.'); if (x) return;
    try { await api('user', '/users/register', { method: 'POST', json: { name: d.get('name').trim(), email, password: d.get('password'), phone: d.get('phone').trim(), role: d.get('role') } }); await signIn(email, d.get('password')); toast('Account created. Welcome to RevShop.'); }
    catch (e) { ferr(f, msg(e, 'We couldn’t create your account. Check your details and try again.')); }
  },
  async review(f, d) {
    ferr(f, ''); const body = { rating: +d.get('rating'), comment: d.get('comment').trim() }, { pid, rid } = f.dataset;
    try { await api('product', rid ? '/api/products/reviews/' + rid : `/api/products/${pid}/reviews`, { method: rid ? 'PUT' : 'POST', json: body }); toast(rid ? 'Review updated.' : 'Review posted.'); route(); }
    catch (e) { ferr(f, msg(e, 'We couldn’t save your review — try again.')); }
  },
  async ship(f, d) {
    clr(f); ferr(f, ''); let x = false; const v = k => d.get(k).trim();
    if (!v('name')) x = bad(f, 'name', 'Enter the recipient’s full name.'); if (!v('address')) x = bad(f, 'address', 'Enter the street address.');
    if (!v('city')) x = bad(f, 'city', 'Enter your city.'); if (!/^\d{6}$/.test(v('pin'))) x = bad(f, 'pin', 'Enter a 6-digit PIN code.'); if (x) return;
    try {
      if (d.get('save')) await api('user', '/users/profile', { method: 'PUT', json: { name: v('name'), phone: S.profile?.phone || '', address: `${v('address')}, ${v('city')} ${v('pin')}` } }).catch(() => { });
      await api('order', '/orders/checkout', { method: 'POST', json: { paymentMethod: d.get('pay') } });
      toast('Order placed. Confirmation sent to your notifications.'); location.hash = '#/orders';
    } catch (e) { ferr(f, msg(e, 'We couldn’t place your order. Check your cart and try again.')); scrollTo(0, 0); }
  },
  async product(f, d) {
    clr(f); ferr(f, ''); let x = false; const price = num(d.get('price')), qty = num(d.get('quantity')), disc = num(d.get('discount') || 0), th = num(d.get('threshold') || 0);
    if (!d.get('name').trim()) x = bad(f, 'name', 'Enter a product name.'); if (!(price > 0)) x = bad(f, 'price', 'Price must be greater than 0.');
    if (!(qty >= 0)) x = bad(f, 'quantity', 'Enter the stock on hand (0 or more).'); if (!(disc >= 0 && disc <= 100)) x = bad(f, 'discount', 'Discount must be between 0 and 100.');
    if (!(th >= 0)) x = bad(f, 'threshold', 'Enter 0 or more.'); if (x) return;
    const id = f.dataset.id, cat = d.get('category');
    const json = { name: d.get('name').trim(), description: d.get('description').trim(), imageUrl: d.get('imageUrl').trim() || null, price, discount: disc, quantity: qty, lowStockThreshold: th, category: cat ? { id: +cat } : null };
    try { await api('product', id ? '/api/products/' + id : '/api/products', { method: id ? 'PUT' : 'POST', json }); $('#dlg').close(); toast(id ? 'Changes saved.' : 'Product added.'); route(); }
    catch (e) { ferr(f, msg(e, 'We couldn’t save this product. Check the details and try again.')); }
  },
  async ostat(f, d) {
    clr(f); const id = +d.get('oid'); if (!(id > 0)) return void bad(f, 'oid', 'Enter the order number.');
    try { await api('order', `/orders/${id}/status?status=${d.get('st')}`, { method: 'PUT' }); toast('Order status updated.'); f.reset(); }
    catch (e) { fail(e, 'We couldn’t update that order. Statuses move one step at a time: Placed → Confirmed → Shipped → Delivered.'); }
  },
  async cat(f, d) {
    clr(f); const name = d.get('cname').trim(); if (!name) return void bad(f, 'cname', 'Enter a category name.');
    try { await api('product', '/api/categories', { method: 'POST', json: { name } }); toast('Category added.'); await cats(true); f.reset(); }
    catch (e) { fail(e, 'We couldn’t add that category — it may already exist.'); }
  }
};
document.addEventListener('submit', async e => {
  const f = e.target.closest('form[data-form]'); if (!f) return; e.preventDefault();
  if (f.dataset.form === 'filter') { const p = new URLSearchParams(); for (const [k, v] of new FormData(f)) if (v) p.append(k, v); location.hash = '#/products' + (p.toString() ? '?' + p : ''); return; }
  const b = e.submitter; b && (b.disabled = true);
  try { await FORMS[f.dataset.form]?.(f, new FormData(f)); } finally { b && (b.disabled = false); }
});
document.addEventListener('change', e => { const f = e.target.closest('form[data-form="filter"]'); if (f && e.target.name !== 'q') f.requestSubmit(); });

/* ---------- router ---------- */
const ROUTES = [[/^\/?$/, home], [/^\/products$/, catalog], [/^\/product\/(\d+)$/, detail], [/^\/cart$/, cart], [/^\/checkout$/, checkout],
  [/^\/orders$/, orders], [/^\/notifications$/, notifs], [/^\/seller$/, seller], [/^\/login$/, login], [/^\/register$/, register]];
async function route() {
  U = claims(); if (!U && S.token) { S.token = null; localStorage.removeItem('rs_token'); }
  const [path, qs] = location.hash.slice(1).split('?'), q = new URLSearchParams(qs || ''), pub = /^\/(login|register)$/.test(path);
  if (!U && !pub) { location.hash = '#/login'; return; } if (U && pub) { location.hash = '#/'; return; }
  if (U && /^\/(cart|checkout|orders)$/.test(path) && U.role !== 'BUYER') { location.hash = '#/'; return; }
  if (U && path === '/seller' && U.role !== 'SELLER') { location.hash = '#/'; return; }
  if (U && !S.name && !S.profile) api('user', '/users/profile').then(p => { S.profile = p; S.name = p.name; nav(path); }).catch(() => { });
  nav(path); document.title = 'RevShop';
  for (const [re, fn] of ROUTES) { const m = path.match(re); if (m) {
    view('<p class="muted">Loading…</p>');
    try { await fn(q, m[1]); } catch (e) { view(`<div class="alert error">${esc(msg(e, 'We couldn’t load this page — try again.'))}</div>`); }
    V.focus(); return; } }
  view('<div class="empty"><p>We couldn’t find that page.</p><a class="btn btn-primary" href="#/">Go home</a></div>');
}
addEventListener('hashchange', route); route();
