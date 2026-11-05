# ROADMAP.md — Plan de modernización por fases

**Proyecto:** Joyería — e-commerce full stack
**Base:** AUDIT.md (auditoría del 2026-10-08)
**Regla:** no avanzar a una fase sin autorización explícita y sin que la anterior deje el proyecto sin errores críticos.

---

## Principios de ejecución

- Commits pequeños y descriptivos (`feat(auth): ...`, `fix(cart): ...`).
- Tras cada cambio importante: compilar, tests, lint, revisar regresiones.
- No romper contratos API existentes sin migración frontend+backend coordinada.
- No eliminar funcionalidades sin justificación documentada.
- No añadir dependencias sin pasar el checklist del prompt (§39).
- Si un cambio rompe algo: **detener y corregir** antes de continuar.

---

## FASE 0 — Auditoría ✅ (completada 2026-10-08)

**Resultado:** `AUDIT.md`, `ROADMAP.md`, `ARCHITECTURE.md`.
**Verificación dinámica:** lint frontend con 3 errores, build frontend con 2 errores TS, backend no compila (JDK 17 < 25 requerido), Docker no instalado localmente.

---

## FASE 1 — Estabilización ✅ (completada 2026-10-08)

**Objetivo:** builds en verde, arranque posible, eliminar riesgos evidentes. Sin nuevas features.

**Resultado:** builds en verde y **backend arrancado y verificado en vivo** con BD nueva.

- Migración duplicada: se renombró `V2__add_google_id_to_users.sql` → `V8__add_google_id_to_users.sql` (la duplicada era la de Google ID; `V2__align_cart_table_with_entity.sql` se mantiene porque entornos existentes ya la aplicaron como versión 2). Flyway aplica V1→V8 en BD limpia ✅.
- `spring:` duplicado fusionado (C2); como efecto secundario el bloque OAuth2 con `client-id` vacío pasó a activo y rompía el arranque → **eliminado** temporalmente (dependencia en pom.xml conservada; se restaura con credenciales reales en Fase 3).
- Seed restringido a `@Profile("dev")` y sin loguear credenciales; compose con `SPRING_PROFILES_ACTIVE` (C5).
- Bypass de Google eliminado en backend y frontend (C3); errores TS/lint de `Login.tsx`/`Register.tsx` resueltos con esa eliminación (C8) — lint 0 errores.
- Pagos: ownership de pedido (404), idempotencia (no re-procesar pagado), `PaymentRequest` sin datos de tarjeta (C4/C7 parcial) y Checkout sin PAN (F11) ✅.
- Quick wins: `@Valid` en 7 PUT sin validar, null-checks → 401 en `/api/auth/me` y `/api/reviews/check` (B-M7/B-M8), CORS desde `FRONTEND_URL` (B-A10) ✅.
- JDK 25 instalado (Temurin 25.0.1.8) y documentado (I-A7); `.gitignore` += `uploads/`.
- **Verificación final:** `mvn test` 13/13 ✅, `npm run lint` 0 errores ✅, `npm run build` ✅. Smoke tests en vivo: arranque 4.7s, Flyway 8/8, productos/categorías 200, login user+admin con `type=Bearer`, `/me` y endpoint admin 200, sin token → 401, validación PUT → 400 con detalle, pago con pedido inexistente → 404 controlado, header CORS correcto.

**Pendiente explícito:** C6 (rotar `JWT_SECRET` e limpiar historial git) — pospuesto, requiere coordinación; OAuth real (Fase 3); pasarela real (Fase 5).

**Tareas:**
1. Renumerar `V2__align_cart_table_with_entity.sql` → `V8__align_cart_table_with_entity.sql` (C1). *(V8 para no alterar checksums de migraciones ya aplicadas fuera de este entorno; verificar orden de aplicación.)*
2. Fusionar el bloque `spring:` duplicado en `application.yml` (C2).
3. Frontend: corregir 2 errores TS de `Login.tsx` (`window.google`) y 3 errores de lint (C8).
4. Decidir e instalar entorno backend: JDK 25 (recomendado, mantiene `pom.xml`) o bajar `java.version` a 17/21. Documentar en README. (I-A7)
5. Deshabilitar/condicionar `DataInitializer`: `@Profile("dev")` + no loguear credenciales (C5).
6. Retirar el bypass `POST /api/auth/google` hasta implementar verificación real (C3) — desactivar endpoint + frontend, sin romper login clásico.
7. `PaymentService`: no marcar pedidos como pagados sin pasarela + exigir ownership (C4, mitigación mínima de Fase 1; integración completa en Fase 5).
8. Quitar `cardNumber`/`cvv`/`expiry` del flujo frontend→backend (C7 parcial: superficie PCI).
9. Corregir quick wins de seguridad: CORS con `FRONTEND_URL` (B-A10), `@Valid` faltantes (B-M7), null-check de `Authentication` en `/auth/me` y `/reviews/check` (B-M8).
10. `.gitignore`: añadir `uploads/`.
11. Ejecutar: `mvn test`, `npm run lint`, `npm run build`.

**Archivos afectados:** `application.yml`, migración V2→V8, `Login.tsx`, `DataInitializer.java`, `AuthController.java`/`SecurityConfig.java`, `PaymentService.java`/`PaymentController.java`, `Checkout.tsx`, `misc.service.ts`, controllers con PUT sin `@Valid`, `.gitignore`.
**Riesgos:** la renumeración Flyway puede afectar BDs que ya aplicaron un V2 (verificar `flyway_schema_history` en cada entorno); retirar el endpoint Google afecta al frontend (coordinar en mismo commit).
**Criterio de salida:** `mvn test` ✅, `npm run lint` ✅, `npm run build` ✅, backend arranca con BD nueva, sin hallazgos críticos abiertos de AUDIT.md §3 (excepto C6, que requiere coordinación).

---

## FASE 2 — Arquitectura ✅ (completada)

**Objetivo:** limpieza estructural sin cambiar comportamiento visible.

**Estado: COMPLETADA** — commits `b8d1e17`…`fa9396e` (11 commits).

**Resumen de implementación:**
1. ✅ Código muerto backend: `SearchRequest`, `Cart.sessionId`+`findBySessionId`, `validateCouponForOrder`, `createGuestTicket`+regla guest `permitAll` (el frontend nunca la usó) + migración `V9__drop_orphan_columns.sql` (`cart.session_id`, `support_tickets.customer_name/email`, `ticket_responses.is_staff`, `inventory.reference`) — `b8d1e17`.
2. ✅ `@CrossOrigin` eliminado de los 16 controllers, solo CORS central (`B-M15`) — `5a5f167`.
3. ✅ `GlobalExceptionHandler` completo (405/415/400 de tipo, `instanceof FieldError` sin cast ciego) + `SecurityConfig` con `authenticationEntryPoint`/`accessDeniedHandler` en JSON (`B-M5`) — `085f8f1`.
4. ✅ Frontend: `utils/errors.ts` (`getErrorMessage`, 16 archivos convertidos — `F-M5`), `utils/shipping.ts` con constantes compartidas (`F-M4`), dedupe de servicios (`F-M6`: `adminService` sin métodos de entidad, `coupon.service.ts` nuevo, `orderService.getAllOrders(page,size,status)`) — `5208faa`, `445deee`.
5. ✅ Hook `useAbortableEffect` + `signal` en servicios (`product`, `user`) aplicado a `Catalog` y `ProductDetail` (`F-M3`) — `f69e568`.
6. ✅ `ErrorBoundary` montado en `main.tsx` (`F-A3`) — `05d4158`.
7. ✅ 401 sin `window.location`: evento `auth:session-expired` + `SessionWatcher` en `App` + banner en `Login` (`F-A2`); `logout` memoizado — `4c8fac2`.
8. ✅ `tsconfig`: `noUnusedLocals: true`, `vite.config.ts` en `include`, `@eslint/js@^9` declarado en devDependencies — `d3eb047`.
9. ✅ Dead code frontend: `SearchBar.tsx`, `ProductTurntable.tsx`, `three`/`@types/three`, `localStorage['user']` (nadie lo leía) — `fa9396e`.

**Verificación final:** `mvn test` 13/13 ✅, `npm run lint` 0 errores (9 warnings preexistentes) ✅, `tsc --noEmit` ✅, `npm run build` ✅. Smoke tests en vivo: Flyway aplicó V9 (now at v9), `/products` 200, sin token → 401 `{"success":false,"message":"No autenticado"}`, método no soportado → 405 JSON, `text/plain` → 415 JSON, token USER en `/admin/users` → 403 `{"message":"Acceso denegado"}`, login user+admin `type=Bearer`, admin stats 200.

**Pendiente explícito:** C6 (rotar `JWT_SECRET` e limpiar historial git) — pospuesto a Fase 3, requiere tu autorización.

**Tareas:**
1. Backend: eliminar código muerto (`SearchRequest`, `validateCouponForOrder`→integrarlo, `createGuestTicket`, `Cart.sessionId`, columnas huérfanas vía migración V9 si aplica).
2. Unificar CORS: eliminar `@CrossOrigin` de 17 controllers, usar solo `CorsConfigurationSource` (B-M15).
3. Completar `GlobalExceptionHandler` (405/415/400 de tipo, sin cast ciego a `FieldError`) + error handler de seguridad con JSON (B-M5).
4. Frontend: extraer `getErrorMessage()` a `utils` (F-M5), constantes de envío compartidas (F-M4), deduplicar servicios admin vs entidad (F-M6).
5. Frontend: hooks personalizados para datos (`useAbortableFetch`) y estados de carga (F-M3).
6. Frontend: `ErrorBoundary` a nivel App (F-A3).
7. Interceptor 401 sin `window.location`: evento + redirección controlada con mensaje (F-A2).
8. Ordenar `tsconfig` (`noUnusedLocals` cuando sea viable, incluir `vite.config.ts`), declarar `@eslint/js`.
9. Eliminar código muerto: `SearchBar.tsx`, `ProductTurntable.tsx`, `three`, `@types/three`, estado `localStorage['user']`.

**Riesgos:** refactor de servicios frontend toca muchas páginas → verificar rutas manualmente tras cambios; nada de cambios de contratos API.
**Criterio de salida:** builds/tests/lint en verde; ningún endpoint cambia de contrato; código muerto eliminado; comportamiento idéntico.

---

## FASE 3 — Seguridad

**Objetivo:** autenticación robusta y autorización correcta.

**Tareas:**
1. ✅ Google OAuth: **sin credenciales en el entorno** → flujo deshabilitado documentado; eliminado `spring-boot-starter-oauth2-client` sin uso; `.env.example` explica cómo habilitarlo (B-A1, B-A2).
2. ✅ Refresh tokens con rotación + reuse detection + logout real + invalidación al cambiar/restablecer contraseña (B-M12): migración `V10__refresh_tokens`, `RefreshTokenService` (hash SHA-256, expiración 7 días, revocación de la familia ante reuse), `POST /api/auth/refresh` y `POST /api/auth/logout`, interceptor axios con cola de peticiones para refresh concurrente único.
3. ✅ IDOR tickets: ownership en lectura/escritura con el email del JWT (B-A3).
4. ✅ Cookies: **decisión — mantener tokens en `localStorage`** por ahora; el riesgo XSS queda mitigado con CSP estricta (`script-src 'self'`), HSTS y sesiones de access cortas con refresh. Migrar a cookie `httpOnly` exige `withCredentials` en axios, `allowCredentials` en CORS y protección CSRF — queda como mejora futura documentada (F-A4).
5. ✅ `nginx.conf`: CSP, HSTS, X-Frame-Options, Referrer-Policy, Permissions-Policy, nosniff y gzip; el script inline del tema se movió a `public/theme-init.js` para permitir CSP estricta (I-M1, F-A4).
6. ✅ Rate limit: XFF solo se acepta desde proxies de confianza (`TRUSTED_PROXIES`, IPs exactas o CIDR), regla de cupones corregida a `POST|/api/coupons/validate`, limpieza determinista por intervalo (I-A4, B-M1).
7. ✅ (local) Rotación JWT_SECRET + limpieza de historial git (C6): el secreto hardcodeado `YXJj…MjAyNg==` (initial import `07b2c68`) fue **purgado de los 43 commits** con `git filter-branch` (verificado: `git rev-list --all` + `git log -p` sin coincidencias, `git fsck` OK, tree de HEAD idéntico al pre-rewrite). Backup del repo espejo en `Temp\opencode\joyeria-backup.git`. **⚠️ Falta el push:** la credencial activa (`sixzyy`) no tiene permisos de escritura en `foxtrot0367/joyeria-main-mobile` (403). Con una cuenta autorizada ejecutar: `git push --force-with-lease origin main` (tracking ya apunta al remoto actual). El secreto purgado se considera **comprometido**: generar uno nuevo por entorno (`README.md`) y reemplazarlo en cualquier despliegue que lo use.
8. ✅ `DataInitializer` solo dev (Fase 1, verificado) + política de contraseñas letra+número (validación `@Pattern` en backend y `pattern` en frontend) + mensaje de registro uniforme para email/teléfono existentes, mitigado con rate limit 5/min (C5, B5).
9. ✅ Reseñas: moderación `PENDING` por defecto y verificación de compra en pedidos pagados (B-A5).

**Tests de seguridad (37 en verde):** AuthService (8), RefreshTokenService (7), RateLimitFilter (6), SupportService (5), CouponService (4), OrderService (4), ReviewService (3).

**Riesgos:** cambios en el flujo de auth afectan todas las sesiones; el paso a cookies httpOnly exige ajustar el interceptor axios; rotar el secreto invalida tokens vigentes.
**Criterio de salida:** ✅ sin bypass de auth; sin IDOR en endpoints existentes; logout real funcional (smoke en vivo: login → refresh → reuse = 401 → logout → refresh = 401); tests de seguridad backend añadidos. Pendiente: tarea 7 (C6) con autorización del usuario.

---

## FASE 4 — E-commerce ✅ (completada 2026-10-11)

**Objetivo:** catálogo, carrito, checkout y pedidos sólidos.

**Tareas:**
1. ✅ Catálogo: topes de `page/size` (`MAX_PAGE_SIZE=50`) y `sort` whitelist (400 fuera de lista) en `ProductService.buildPageable`; `ProductRepository.findFiltered` null-safe; migración `V11` con `pg_trgm` + índices GIN trigram (B-M3).
2. ✅ Carrito: stock acumulado y cantidad ≥1 validados en `CartService.addItem`; `CartItemDTO.productStock` expuesto; frontend deshabilita "−" en cantidad 1 y "+" en stock máximo, con banner de error y estado busy (B3, F-M1, B-B3).
3. ✅ Checkout: validación por campo (`validateShipping`), bloqueo de avance de paso y revalidación antes de pagar (C7).
4. ✅ Cupones: `CouponService.getValidCoupon` (activo, vigencia, `maxUses`) + chequeo de `minAmount` en `OrderService.resolveCoupon`; normalización `trim().toUpperCase()`; `uses_count` incrementado al consumir (B-A7).
5. ✅ Stock: `ProductRepository.findByIdForUpdate` con bloqueo pessimista (`@Lock(PESSIMISTIC_WRITE)`) en `createOrder`, evita sobreventa lógica (B-A6).
6. ✅ Pedidos: `OrderTimeline` (PENDING→PAID→PREPARING→SHIPPED→DELIVERED + banner CANCELLED) en confirmación; dashboard cuenta ingresos solo de pagos `COMPLETED` (`sumTotalByPaymentStatus`) (B-M11).
7. ✅ Favoritos: rating real y selector de cantidad limitado por stock en detalle; mover al carrito desde la lista (F-M9).
8. ✅ Email transaccional real por SMTP (`spring-boot-starter-mail`) con fallback honesto si no está configurado; `OrderCreatedEvent` (`@TransactionalEventListener AFTER_COMMIT`) y reset de contraseña; confirmación de pedido sin prometer correo (B-A4).
9. ✅ Precios: backend como única fuente de verdad vía `GET /api/cart/summary` (`CheckoutSummaryDTO`); el checkout consume totales del backend y ya no los calcula (F4.9).
- ✅ Extra: corregido `CartService` devolvía el carrito sin el ítem recién agregado en carrito nuevo (colección inversa `mappedBy` desactualizada); ahora `mapToDTO` consulta `CartItemRepository.findByCartId` (commit `f3f9061`).

**Tests (59 en verde):** AuthService (8), RefreshTokenService (7), RateLimitFilter (6), SupportService (5), CouponService (4), OrderService (13), ReviewService (3), ProductService (7), CartService (3), EmailService (5). Frontend: `npm run lint` (0 errores, 9 warnings preexistentes) y `npm run build` OK.

**Criterio de salida:** ✅ smoke en vivo contra BD local: registro → catálogo (paginación/sort y validaciones 400) → carrito (stock acumulado, sobre-stock 400) → `/cart/summary` → pedido (totales backend, stock 17→15 y `soldCount` +2) → cupón (10% aplicado, `minAmount`/expirado 400, `uses_count` +1) → logout. Sin `quantity: 0`; sin sobreventa lógica.

---

## FASE 5 — Pagos ✅ (completada 2026-10-11)

**Objetivo:** integración Mercado Pago real (checkout pro + webhook).

**Tareas:**
1. ✅ Backend: preferencia de Checkout Pro (`POST /api/payments/checkout`); estados de pago con idempotencia (reutiliza pago `PENDING` por pedido; webhook y confirmación idempotentes por `preference_id`); webhook con verificación de firma HMAC-SHA256; reconciliación de monto + moneda contra el pago registrado (C4 completo).
2. ✅ Eliminado `PaymentRequest` con datos de tarjeta: el checkout es tokenizado por la pasarela, cero PAN en nuestros servidores; frontend solo envía `{orderId, paymentMethod}` (C7 PCI).
3. ✅ `POST /api/payments/webhook` implementado (ya no es 404 huérfano); `SecurityConfig` permite `/api/payments/webhook` y `/api/payments/webhook/**`.
4. ✅ Estados sincronizados con la pasarela: `approved→PAID/APPROVED`; `rejected/cancelled/refunded→rechazo`; solo la pasarela (webhook firmado o confirmación sandbox) marca un pedido como `PAID`.
5. ✅ Sandbox: `SandboxPaymentProvider` (por defecto en dev, rechazado en `prod`) simula la pasarela sin cobro real; `MercadoPagoPaymentProvider` real listo para credenciales; **sin claves `MERCADOPAGO_*` no se inventan** — queda preparado y documentado.
- ✅ Extra: eliminados `POST /api/payments/process` y `GET /api/payments/reference/{ref}` (IDOR C4); paquete `integration.payment` con `PaymentProvider`/`PaymentContext`/`PaymentPreference`; migración `V12__payment_preference.sql` (`preference_id` + índices único `ux_payments_preference_id`, `idx_payments_provider_reference`, `idx_payments_order_id`).
- ✅ Frontend: `paymentService.createCheckout/getStatus/confirmSandbox`; `Checkout.tsx` redirige a la pasarela (MP) o a la página de pago simulada; nueva `SandboxPaymentPage` (`/pago/sandbox`); `OrderConfirmation` refleja el estado de pago con sondeo automático mientras está `PENDING`.
- ✅ Mapeo de `PaymentProcessingException` a HTTP 400 en `GlobalExceptionHandler`.

**Tests (81 en verde, +22):** PaymentService (13), MercadoPagoSignatureVerifier (4), SandboxPaymentProvider (2), MercadoPagoPaymentProvider (3) sobre los 59 previos. Frontend: `npm run lint` (0 errores, 9 warnings preexistentes) y `npm run build` OK.

**Verificación E2E (sandbox, PostgreSQL local, `V12` aplicada):** registro→carrito→pedido (`PENDING`)→`checkout` (provider `sandbox`, `checkoutUrl` `/pago/sandbox?ref=…`, **no** marca pagado)→`sandbox/confirm` (pedido `PAID`/pago `APPROVED`)→`status` (`PAID`/`APPROVED`); confirmación y re-checkout idempotentes; webhook con firma inválida = `400` (no 404); `status` inexistente = `404`.

**Riesgos:** el webhook real requiere `MERCADOPAGO_ACCESS_TOKEN` + `MERCADOPAGO_WEBHOOK_SECRET` y URL pública; los estados dependen de la notificación de MP (mitigado con consulta activa `GET /v1/payments/{id}`).
**Criterio de salida:** ✅ flujo de pago sandbox verificado extremo a extremo; el webhook (y solo él, o la confirmación sandbox) actualiza el pedido; no es posible marcar `PAID` sin pasar por la pasarela.

---

## FASE 6 — Admin ✅ (completada 2026-10-11)

**Objetivo:** dashboard con métricas correctas y CRUDs profesionales.

**Tareas:**
1. ✅ Dashboard: ventas del día/mes (solo pedidos pagados), pedidos, clientes, top productos, stock bajo — corrigiendo B-M11. `DashboardStatsDTO` amplía `todayRevenue`/`monthRevenue` y `topSellingProducts` (top 5 real por `soldCount`); `ordersByStatus` con todos los estados.
2. ✅ CRUD productos con imágenes: subida local a `uploads/` (volumen) vía `POST /api/admin/uploads` (`FileStorageService`, validación de tipo, `/uploads/**` servido por `WebConfig`); si hay credenciales Cloudinary la implementación es sustituible.
3. ✅ Pedidos: máquina de estados (ya existente) + `GET /api/admin/orders/{id}` con cliente/items y vista de detalle con acciones de transición válidas (transiciones inválidas → 400).
4. ✅ Usuarios: roles y activar/desactivar, **protegiendo el último admin** (B-B2): no se puede degradar ni desactivar al último `ADMIN` activo (→ 400).
5. ✅ Soporte admin: hilos de conversación con `GET /api/admin/support/tickets/{id}` y `POST /api/admin/support/tickets/{id}/responses` (B-M6); `SupportTicketDTO` incluye `responses`; el cliente ve las respuestas desde `/soporte`.
6. ✅ Reseñas: moderación funcional con `GET /api/admin/reviews?status=` (filtro pendientes/aprobadas/rechazadas/todas) + aprobar/rechazar; la creación ya exige compra previa y queda `PENDING`.

- ✅ Extras backend: `TopProductDTO`, `TicketResponseDTO`, `UploadResponse`; consultas de repositorio para métricas (`countGroupByStatus`, `sumTotalByPaymentStatusSince`, contadores de stock/usuarios); `findAllByOrderByCreatedAtDesc` de reseñas.
- ✅ Frontend: `Dashboard` (tarjetas + más vendidos), `AdminProducts` (subida de imágenes con previsualización), `AdminOrders` (detalle), `AdminUsers` (mensaje de error del último admin), `AdminSupport` (conversación + responder), `AdminReviews` (filtros + paginación), `SupportPage` (mis tickets + respuestas), `admin.service`/`order.service`/`user.service`.

**Tests (95 en verde, +14 sobre Fase 5):** `FileStorageServiceTest` (4), `UserServiceTest` (7), `SupportServiceTest` (5) — más `OrderServiceTest` y `ReviewServiceTest` ampliados. Frontend: `npm run lint` (0 errores, 10 warnings preexistentes) y `npm run build` OK.

**Verificación E2E (PostgreSQL local, perfil `dev`, smoke6):** login admin → dashboard con métricas reales (pedidos, ingresos hoy/mes, clientes, stock bajo, top 5, por estado); subida de imagen (`200`, servida en `/uploads/...`) y producto creado con imagen; flujo comprador registro→carrito→pedido→`sandbox/confirm` (`PAID`); detalle admin con cliente/items; transición inválida `PAID→DELIVERED` = `400`, válidas `PREPARING→SHIPPED→DELIVERED` = OK; degradar/desactivar último admin = `400`; ticket creado por cliente y respondido por admin (`responses=1`, `staff=true`) visible en el cliente; reseña creada `PENDING`, visible y aprobada por admin.

**Criterio de salida:** ✅ el admin opera el ciclo completo de negocio (productos con imágenes, pedidos y estados, usuarios, soporte, reseñas) sin tocar BD; métricas cuadran con datos reales.

---

## FASE 7 — UI/UX ✅ (completada 2026-10-11)

**Objetivo:** dirección visual premium de joyería (elegante, minimalista, editorial; dorado como acento).

**Tareas:**
1. ✅ Base premium consolidada: tipografía editorial (Playfair Display + Inter), paletas ivory/espresso con `darkMode`, tokens HSL de marca (`primary`, `surface`, `foreground`, `line`), `prefers-reduced-motion` respetado y microinteracciones suaves (framer-motion) consistentes en Home, catálogo, producto, carrito, checkout, cuenta y admin.
2. ✅ Imágenes robustas: nuevo componente compartido `SmartImage` con placeholder del diamante AURA + `onError` + `loading="lazy"`/`decoding="async"` (F-M8, F-M14); sustituye el código duplicado en `ProductCard`, `Showcase`, `QuickViewModal` y corrige el fallback roto de `ProductDetail` (antes dejaba `src=''`) y los thumbnails de carrito/checkout.
3. ✅ Modal accesible (F-M13): `role="dialog"`, `aria-modal`, `aria-labelledby`, cierre con `Escape`, trampa de foco (Tab/Shift+Tab), foco inicial y restauración del foco al cerrar.
4. ✅ `document.title` por ruta (F-M10): hook `useDocumentTitle` aplicado en todas las páginas de tienda/cuenta/legal (Home, catálogo, producto, carrito, checkout, login/registro/recuperación, cuenta, pedidos, direcciones, favoritos, soporte, FAQ, legales) + sitemap ampliado (`/personaliza`).
5. ✅ Newsletter real en el Home (F-M7): `newsletterService.subscribe` con estado de envío y feedback de éxito/error (antes solo mostraba un toast falso).
6. ✅ 404 dentro del layout: la ruta `*` vive bajo `MainLayout` (mantiene Navbar/Footer) con título propio.
7. ✅ Consentimiento de cookies con efecto real (F-M14): se guarda `{ type, ts, version }` y se emite el evento `cookie-consent` consumible por analítica.
8. ✅ Admin responsive (F-A5): sidebar colapsable con barra superior y drawer en móvil (overlay + cierre con Escape), contenido con padding adaptativo; se conserva la navegación por secciones.

**Riesgo:** no romper funcionalidad existente al tocar componentes; verificar cada página tras rediseñarla.

**Verificación:** `npm run lint` (0 errores, 10 warnings preexistentes `react-hooks/exhaustive-deps`), `npm run build` (tsc + vite) OK. Cambios solo de frontend; sin migraciones ni cambios de API.

---

## FASE 8 — Mobile ✅

**Objetivo:** experiencia diseñada para móvil (360–430px), no adaptada.

**Tareas:** sidebar admin responsive con hamburguesa (F-A5); revisar navbar, búsqueda, cards, carrito, checkout, formularios, modales; eliminar overflow horizontal; targets táctiles ≥44px; quick view accesible en móvil; `loading="lazy"` (F-M14).

**Implementado:**
1. ✅ Guard global de overflow horizontal (`overflow-x: clip` en `body`); no rompe `sticky` ni el scroll interno de los carruseles (`overflow-x-auto` propios).
2. ✅ `ProductCard`: botón "Vista rápida" ahora **visible en táctil** (antes solo con `group-hover`, inaccesible sin puntero); en desktop sigue apareciendo al hover. Botón de favoritos a ≥44px en móvil.
3. ✅ Navbar: botón hamburguesa y accesos a favoritos/carrito con objetivo táctil de 44px.
4. ✅ Carrito y ficha de producto: controles de cantidad (−/+) y botón eliminar a ≥44px.
5. ✅ `QuickViewModal`: botón de cerrar y selector de cantidad a ≥44px; accesible en móvil (columnas apiladas, `max-h-[92vh]` con scroll) y cierre con `Escape`/overlay.
6. ✅ `AdminProducts`: formulario a una columna en móvil (`grid-cols-1 sm:grid-cols-2`) y acciones de fila a ≥44px. Las tablas del panel ya usan `overflow-x-auto` (scroll contenido sin desbordar la página).
7. ✅ `loading="lazy"` + `decoding="async"` en imágenes vía `SmartImage` (F-M14), heredado de Fase 7.

**Verificación:** `npm run lint` (0 errores, 10 warnings preexistentes `react-hooks/exhaustive-deps`), `npm run build` (tsc + vite) OK. Guardas automáticas de overflow y objetivos táctiles aplicadas en los controles clave; **se recomienda una verificación manual final en responsive 360/375/390/430px** (no ejecutable en este entorno sin navegador). Cambios solo de frontend; sin migraciones ni cambios de API.

---

## FASE 9 — Performance ✅

**Objetivo:** optimización medida, sin premature optimization.

**Tareas:** revisar N+1 (B-M2), OSIV off + `@Transactional`/`JOIN FETCH` (B-M4), índices (B-B6), tamaños de página; frontend: debounce de búsqueda, memo donde aporte, imágenes responsive/WebP (Cloudinary si hay credenciales), revisar chunks. Medir antes/después.

**Implementado:**
1. ✅ OSIV desactivado (`spring.jpa.open-in-view: false`) y `hibernate.default_batch_fetch_size: 30` para mitigar N+1 en colecciones lazy (B-M4/B-M2). Con OSIV off, se añadieron transacciones de lectura `@Transactional(readOnly = true)` a nivel de clase en los servicios que mapean asociaciones lazy: `ProductService`, `OrderService`, `FavoriteService`, `SupportService`, `CategoryService`, `ReviewService`; `CartService` (readOnly en clase, `@Transactional` read-write en `getCart` porque persiste el carrito vacío); `@Transactional` en métodos de escritura de `CategoryService`/`AddressService`/`ReviewService`/`ProductService.deleteProduct`; `PaymentService.getPaymentStatus` readOnly.
2. ✅ N+1 de counts en `CategoryService.getAllCategories()` eliminado: nueva consulta `ProductRepository.countActiveGroupedByCategory()` (una sola query agrupada) y `mapToDTO(Category, long)`.
3. ✅ Índices faltantes de claves foráneas (B-B6) en `V13__favorite_fk_indexes.sql`: `support_tickets.user_id`, `ticket_responses.ticket_id`, `reviews.user_id` y UNIQUE `reviews(user_id, product_id)` (con deduplicación previa).
4. ✅ Frontend: `ProductCard` envuelto en `memo`; `Catalog` con callbacks estables (`useCallback`) y **debounce de 400 ms** en los inputs de precio (evita una petición por pulsación); `AuthContext`/`CartContext` con callbacks `useCallback` y value del provider `useMemo`. Los chunks ya estaban separados (`manualChunks`: react/motion/icons).
5. ✅ Tamaños de página acotados por `ProductService.MAX_PAGE_SIZE = 50` (heredado de Fase 4).

**Verificación:** `mvn -o test` **95/95** ✅. Arranque real con OSIV off contra PostgreSQL (Flyway migró a **v13**). Smoke E2E completo (`smoke6.ps1`) OK: login, dashboard, upload, producto, compra + pago sandbox, detalle admin, transiciones, protección último admin, soporte, reseñas. Rutas lazy bajo OSIV off (`/api/categories` con `productCount`, `/api/admin/orders` con items, `/api/favorites`, `/api/cart`) → 200 sin `LazyInitializationException`. `npm run lint` (0 errores, 10 warnings preexistentes), `npm run build` OK.

---

## FASE 10 — Testing ✅

**Objetivo:** cobertura de comportamiento crítico.

**Tareas:** backend — tests de `OrderService` (camino feliz), `PaymentService`, validación de cupones, controllers MockMvc, seguridad (ownership/roles); migraciones con Testcontainers o H2+Flyway; JaCoCo. Frontend — instalar Vitest, tests de `authService`/`CartContext`/componentes clave. E2E mínimo (Playwright): registro→login→producto→carrito→checkout→pedido.

**Implementado:**
1. ✅ Backend — JaCoCo `0.8.15` en `pom.xml` (`prepare-agent` + `report` en fase `test`, compatible con JDK 25). Reporte en `target/site/jacoco/`.
2. ✅ Backend — 4 nuevas clases de test MockMvc standalone (`MockMvcBuilders.standaloneSetup(...).setControllerAdvice(new GlobalExceptionHandler())`) en `com.joyeria.controller`: `OrderControllerTest` (7, incl. ownership y rol admin), `AuthControllerTest` (5), `ProductControllerTest` (6), `PaymentControllerTest` (6) = **24 tests nuevos**.
3. ✅ Frontend — Vitest `3.2.7` + `@vitest/coverage-v8` + `jsdom` + Testing Library (`react`, `jest-dom`, `user-event`). Config en `vitest.config.ts` (jsdom, `setupFiles`, exclusión de `e2e/**`) y `src/test/setup.ts` (limpieza tras cada test + polyfills de `matchMedia`/`IntersectionObserver`/`ResizeObserver` para framer-motion). Scripts `test` y `test:coverage`.
4. ✅ Frontend — 4 archivos de test (**20 tests**): `src/utils/errors.test.ts` (5), `src/services/auth.service.test.ts` (5), `src/contexts/CartContext.test.tsx` (5), `src/components/ProductCard.test.tsx` (5).
5. ✅ E2E Playwright `@playwright/test` (`e2e/purchase-flow.spec.ts`, `playwright.config.ts`, script `test:e2e`): camino feliz registro → catálogo → carrito → checkout (envío) → pago → pasarela sandbox → `/pedido-confirmado`. Precarga el consentimiento de cookies vía `addInitScript`.

**Verificación:** `mvn test` **119/119** ✅ (cobertura actual: instrucciones **39.2 %**, ramas **38.6 %**; no se fija umbral mínimo para no romper la build). `npm test` **20/20** ✅. `npm run test:e2e` **1/1** ✅ (contra dev server 5173 + backend 8080). `npm run lint` (0 errores, 10 warnings preexistentes), `npm run build` OK.

**Criterio de salida:** `mvn test` con cobertura de los flujos críticos ✅; al menos un E2E del camino feliz ✅.

---

## FASE 11 — CI/CD ✅

**Objetivo:** GitHub Actions en cada PR.

**Tareas:** workflow con install → lint → tests → build frontend → build backend → (opcional) docker build. Branch protection documentado. Opcional: dependency review.

**Implementado:**
1. ✅ **NUEVO** `.github/workflows/ci.yml`. Disparadores: `push` a `main` y `pull_request` hacia `main`; `concurrency` con cancelación en progreso; `permissions: contents: read`.
   - Job **backend** (JDK 25 temurin, cache Maven): `mvn -B -ntp verify` (compila, 119 tests, empaqueta el jar) + subida del informe JaCoCo como artefacto.
   - Job **frontend** (Node 20, cache npm): `npm ci` → `npm run lint` → `npm run test:coverage` → `npm run build` + subida de la cobertura.
   - Job **docker** (solo en `push`, `needs: [backend, frontend]`): construye las imágenes `joyeria-backend:ci` y `joyeria-frontend:ci` para validar los `Dockerfile` (no se publica).
2. ✅ **Branch protection documentado** en `README.md` (§ Integración continua): requerir PR, exigir los checks `Backend (Java 25 / Maven)` y `Frontend (Node 20 / Vite)`, ramas al día, aprobaciones opcionales. Requiere permisos de administrador del repositorio.
3. ✅ (Opcional) `dependency-review` documentado como mejora futura (requiere habilitar el grafo de dependencias).

**Verificación (comandos del workflow ejecutados localmente):** `mvn -B -ntp verify` → BUILD SUCCESS, `Tests run: 119`, jar generado; `npm ci` OK (lockfile sincronizado); `npm run lint` 0 errores/10 warnings; `npm run test:coverage` 20/20; `npm run build` OK. Etiquetas Docker de los `Dockerfile` verificadas como existentes en Docker Hub (`maven:3.9.15-eclipse-temurin-25-alpine`, `eclipse-temurin:25-jre-alpine`, `node:20-alpine`, `nginx:alpine`).

**Nota:** la rama `main` actual ya no falla lint/build (quedó en verde desde Fase 1); el workflow no requiere base de datos (no hay tests `@SpringBootTest`).

---

## FASE 12 — Producción ✅

**Objetivo:** despliegue real con seguridad perimetral.

**Tareas:** perfiles dev/prod en compose (exponer solo 80/443, no 5432/8080), TLS (certbot/traefik o Caddy), `.env` con secretos fuertes, volumen `uploads`, healthchecks correctos (actuator `/actuator/health` — requiere añadir starter), logs estructurados sin DEBUG, backups de Postgres, README completo (I-M5), LICENSE, rotación de secretos verificada (C6), Spring Boot 4.x (B-A8) como decisión explícita.

**Implementado:**
1. ✅ **Actuator/health**: añadido `spring-boot-starter-actuator`; `management.endpoints.web.exposure.include: health` (solo health) con probes `liveness`/`readiness` y `show-details: never`. `SecurityConfig` permite `/actuator/health/**` sin autenticación. El indicador de salud de correo (`MailHealthIndicator`) está ligado a `EMAIL_ENABLED` (`management.health.mail.enabled: ${EMAIL_ENABLED:false}`) para evitar que `/actuator/health` reporte DOWN cuando el correo es opcional.
2. ✅ **Compose prod-safe**: `docker-compose.yml` ya **no publica** `5432`/`8080`; solo Caddy (perfil `tls`) expone `80`/`443`. Servicios con `restart: unless-stopped`, `depends_on` con `condition: service_healthy`, red interna dedicada y secretos obligatorios vía `${VAR:?}`. **NUEVO** `docker-compose.dev.yml` (publica puertos + perfil `dev`) y **NUEVO** `Caddyfile` (TLS automático para `DOMAIN`).
3. ✅ **Volumen `uploads`** (`uploads:/app/uploads`); `Dockerfile` del backend pre-crea `/app/uploads` con propietario `joyeria` (el contenedor corre como no-root) para que el volumen sea escribible.
4. ✅ **Healthchecks coherentes**: `Dockerfile` backend y compose usan `GET /actuator/health`; frontend usa `GET /` (nginx).
5. ✅ **Logs sin DEBUG**: **NUEVO** `application-prod.yml` con `root: INFO`, `org.springframework`/`hibernate` en `WARN`, patrón de consola y `show-details: never`.
6. ✅ **Backups de Postgres**: **NUEVOS** `scripts/backup-db.sh` (pg_dump comprimido + retención 30 días) y `scripts/restore-db.sh`. Programables con cron/Task Scheduler.
7. ✅ **Rotación de secretos (C6)**: `JWT_SECRET` obligatorio sin valor por defecto (el arranque falla si falta); procedimiento de rotación documentado en el README.
8. ✅ **README completo (I-M5)** + **LICENSE** (MIT) + `.env.example` ampliado (dominio, ACME, proxies confiables, nivel de log) + `.gitattributes` (LF para scripts).
9. ✅ **Decisión Spring Boot 4.x (B-A8)**: documentada explícitamente en el README — se permanece en 3.5.x y la migración a 4.0 se abordará como iniciativa independiente.

**Verificación:** `mvn test` **119/119** ✅. Arranque real con perfil `dev` y con perfil `prod` contra PostgreSQL → `GET /actuator/health` **200** (`{"status":"UP","groups":["liveness","readiness"]}`); `/actuator/env` y `/actuator/beans` **no expuestos**; `/api/products` **200**. Compose y override validados como YAML; tags de las imágenes verificados en Docker Hub. (Docker no está instalado en el entorno de desarrollo, por lo que `docker build`/`compose up` se validan en CI y en el host de despliegue.)

---

## Resumen de dependencias entre fases

```
0 auditoría → 1 estabilización → 2 arquitectura → 3 seguridad
                                    ↘ 4 e-commerce → 5 pagos → 6 admin
                                                    ↘ 7 UI/UX → 8 mobile → 9 performance
                     10 testing (paralelo desde Fase 3) → 11 CI/CD → 12 producción
```

- **Fase 1 es bloqueante** para todo lo demás (nada compila hoy).
- CI/CD (11) solo tiene sentido con builds en verde (Fase 1) — puede adelantarse a continuación de Fase 2 si lo deseas.
- Testing (10) puede iniciar en paralelo desde Fase 3.

---

## Fase IA — Asistente de compras con IA (AURA)

Iniciativa posterior a las 13 fases, sin alterar el stack (Spring Boot + React +
PostgreSQL) ni introducir dependencias nuevas.

**Principio rector:** la IA **solo** interpreta la intención y redacta texto; los
productos, precios, stock y enlaces provienen siempre de PostgreSQL. Nunca
inventa datos comerciales.

**Implementado:**
1. ✅ **Auditoría FASE 0** (`docs/AI_SHOPPING_ASSISTANT_AUDIT.md`).
2. ✅ **Backend** — paquete `com.joyeria.ai`: `AiAssistantController`
   (`POST /api/ai/assistant`, público), `AiAssistantService` (orquestación +
   fallback), `SearchIntentParser`/`SearchIntent`/`TextNormalizer` (intención
   determinista), `ProductSearchService` (consulta real),
   `ProductRecommendationService` (scoring), `AiPromptService`, `AiProperties`,
   `AiUnavailableException`, `client/**` (`RestClient` compatible OpenAI +
   `DisabledAiChatClient`) y `dto/**`.
3. ✅ **Integración**: `ProductService.mapProducts`, `SecurityConfig` (permitAll),
   `RateLimitFilter` (20/60 s por IP), bloque `ai:` en `application.yml`,
   `.env.example` (`AI_*` + `VITE_AI_ASSISTANT_ENABLED`).
4. ✅ **Frontend**: `services/ai.service.ts`, `components/AiAssistantWidget.tsx`
   (botón flotante + chat + sugerencias + agregar al carrito), montado en
   `MainLayout.tsx`; tipos y `vite-env.d.ts` actualizados.
5. ✅ **Modo por defecto determinista** (`AI_ENABLED=false`), sin proveedor externo
   y sin claves en el cliente.
6. ✅ **Documentación**: `docs/AI_SHOPPING_ASSISTANT.md`.

**Verificación:** `mvn -o test` **144/144** ✅ (25 nuevos, sin regresiones);
`npm run lint` 0 errores; `npm test` **26/26** ✅; `npm run build` correcto.

---

## Fase IA — Smart Gift Finder

Iniciativa posterior a las 13 fases, sin alterar el stack (Spring Boot + React +
PostgreSQL) ni introducir dependencias ni migraciones nuevas.

**Principio rector:** el motor de recomendación **solo** ordena y explica; los
productos, precios, materiales, imágenes y stock provienen siempre de PostgreSQL.
Nunca inventa datos comerciales ni oculta productos agotados.

**Decisión de diseño (Opción D):** sin cambios de esquema; afinidades de
ocasión/estilo/destinatario deducidas del texto real ya almacenado.

**Implementado:**
1. ✅ **Auditoría FASE 0** (`docs/GIFT_FINDER_AUDIT.md`).
2. ✅ **Backend** — paquete `com.joyeria.gift`: `GiftFinderController`
   (`POST /api/gift-finder/recommend`, público), `GiftFinderService` (scoring
   determinista + consulta real + explicaciones) y `dto/**`
   (`GiftFinderRequest`, `GiftRecommendation`, `GiftFinderResponse`).
   Reutiliza `TextNormalizer`, `ProductRepository.searchFiltered` y
   `ProductService.mapProducts`.
3. ✅ **Integración**: `SecurityConfig` (permitAll), `RateLimitFilter` (20/60 s
   por IP).
4. ✅ **Frontend**: `services/gift.service.ts`, `pages/GiftFinder.tsx`
   (asistente de 5 pasos, URL compartible, resultados reales + agregar al
   carrito), ruta en `App.tsx` y enlace en `Navbar.tsx`; tipos en
   `types/index.ts`.
5. ✅ **Motor determinista**: presupuesto siempre respetado; disponibilidad
   siempre evaluada; relevancia orientativa (nullable sin criterios).
6. ✅ **Documentación**: `docs/GIFT_FINDER.md`.

**Verificación:** `mvn -o test` **159/159** ✅ (15 nuevos, sin regresiones);
`npm run lint` 0 errores; `npm test` **35/35** ✅ (9 nuevos); `npm run build`
correcto.
