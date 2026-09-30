# RevShop web (Angular 20 + TypeScript)

Standalone components, lazy-loaded routes, typed API layer, JWT interceptor, route guards, reactive forms.

## Run (development)
```bash
npm install
npm start                      # http://localhost:4200, proxies /svc/* to the services (8081-8086)
GATEWAY=http://127.0.0.1:9000 npm start    # or send everything through the API gateway
```
Start Eureka/config-server and the services first. The proxy removes the need for CORS on the backend.

## Build (production)
```bash
npm run build                  # -> dist/revshop-web/browser
docker build -t revshop-web .  # nginx image; /gw/* is proxied to http://api-gateway:9000
```
Production calls go to `/gw/...` (see `src/environments/environment.prod.ts` and `nginx.conf`).
Change the host in `nginx.conf` if your gateway isn't called `api-gateway`.

## Gateway routes required
`/users/**`, `/api/products/**`, `/api/categories/**`, `/cart/**`, `/orders/**`, `/payments/**`, `/notifications/**`
(your `application.properties` in the config repo currently only defines product and user routes).

## Layout
- `src/app/core` – models, `Api` (one method per endpoint), `Auth` (JWT + role), interceptor, guards, error mapping
- `src/app/shared` – product card, thumbnail, modal, order summary, status badge
- `src/app/pages` – auth, home, catalog, product-detail, cart, checkout, orders, notifications, seller
