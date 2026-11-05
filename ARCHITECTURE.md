# ARCHITECTURE.md — Arquitectura actual y objetivo

**Proyecto:** Joyería — tienda online de joyería personalizada
**Fecha:** 2026-10-08 (estado tras auditoría Fase 0)

---

## 1. Visión general

```
┌──────────────────────────────┐
│  joyeria-frontend (SPA)      │  React 18 + TypeScript + Vite 5 + Tailwind
│  :5173 (dev) / :80 (nginx)   │  axios centralizado, contexts, React.lazy
└──────────────┬───────────────┘
               │ HTTPS (pendiente) / JSON + JWT Bearer
┌──────────────▼───────────────┐
│  joyeria-backend (API REST)  │  Spring Boot 3.5 + Spring Security (JWT)
│  :8080  /api/**              │  Capas: controller → service → repository
└──────────────┬───────────────┘
               │ JDBC
┌──────────────▼───────────────┐
│  PostgreSQL 16 + Flyway      │  ddl-auto: validate, migraciones V1..V8
└──────────────────────────────┘
```

Docker Compose orquesta `postgres`, `backend` y `frontend` con healthchecks y volumen `postgres_data`.

---

## 2. Stack tecnológico

| Capa | Tecnología | Versión | Notas |
|---|---|---|---|
| Frontend | React + TypeScript + Vite | 18.3 / 5.6 / 5.4 | `strict: true`, SPA con code splitting |
| Estilos | Tailwind CSS | 3.4 | Tokens semánticos (`surface`, `foreground`, `line`), dark mode |
| Animación | framer-motion | 11.9 | Respeta `prefers-reduced-motion` |
| HTTP | axios | 1.7 | Instancia única `services/api.ts` |
| Router | react-router-dom | 6.26 | Guards `ProtectedRoute`/`AdminRoute` |
| Backend | Spring Boot | 3.5.16 | ⚠️ fin de soporte OSS 2026-06-30 |
| Java | — | 25 (`pom.xml`) | ⚠️ local solo hay JDK 17 |
| Seguridad | Spring Security + JJWT | stateless, BCrypt | Access token JWT (sin refresh aún) |
| ORM | JPA/Hibernate + Flyway | `ddl-auto: validate` | Migraciones SQL versionadas |
| BD | PostgreSQL | 16 | Volumen Docker persistente |
| Infra | Docker Compose | 3 servicios | Multi-stage builds, usuario no-root en backend |

**Dependencias sin uso real detectadas:** `spring-boot-starter-oauth2-client`, `spring-boot-starter-mail` (sin envío real), `three`/`@types/three` (frontend), SDK Mercado Pago ausente pese a la config.

---

## 3. Backend — estructura por capas

```
joyeria-backend/src/main/java/com/joyeria/
├── config/        SecurityConfig, DataInitializer (seed)
├── controller/    21 controllers (REST /api)
├── dto/           ~40 DTOs (request/response) + ApiResponse, PagedResponse
├── exception/     GlobalExceptionHandler, ResourceNotFoundException
├── model/         22 entidades JPA (User, Product, Order, Cart, Coupon…)
├── repository/    18 repositorios Spring Data
├── service/       18 servicios (lógica de negocio)
├── security/      JwtTokenProvider, JwtAuthenticationFilter,
│                  RateLimitFilter, CustomUserDetailsService
resources/
├── application.yml            configuración (⚠️ clave spring: duplicada)
└── db/migration/              Flyway V1..V8 (⚠️ dos V2)
```

**Flujo de una petición:**
```
HTTP → RateLimitFilter → JwtAuthenticationFilter → Controller (@Valid DTO)
     → Service (@Transactional, reglas de negocio) → Repository → PostgreSQL
     → DTO de respuesta → ApiResponse<T>
```

**Reglas actuales de autorización:**
- `/api/auth/**` y catálogo/lecturas: `permitAll`.
- Endpoints de usuario: autenticado (ownership verificado en direcciones, pedidos, pagos y **tickets**).
- `/api/admin/**`: `hasRole('ADMIN')` + `@PreAuthorize` por controlador (doble barrera).

**Inventario API:** 88 endpoints (28 públicos / 28 autenticados / 32 admin). Una ruta `permitAll` sin implementar: `/api/support/tickets/guest` (`/api/payments/webhook/**` implementado en Fase 5; `POST /api/payments/process` y `GET /api/payments/reference/{ref}` eliminados por IDOR). En Fase 6 se añadieron: `POST /api/admin/uploads`, `GET /api/admin/orders/{id}`, `GET /api/admin/support/tickets/{id}`, `POST /api/admin/support/tickets/{id}/responses`, `GET /api/admin/reviews`.

---

## 4. Frontend — estructura

```
joyeria-frontend/src/
├── components/    20 componentes (Navbar, ProductCard, Modal, Rating…)
├── contexts/      AuthContext, CartContext, ThemeContext, ToastContext
├── layouts/       MainLayout, AdminLayout
├── pages/         32 páginas (Home, Catalog, ProductDetail, Checkout,
│                  user/*, admin/*) — todas con React.lazy
├── routes/        ProtectedRoute, AdminRoute
├── services/      api.ts (axios + interceptores) + servicios por entidad
├── types/         tipos de dominio compartidos (index.ts, 228 líneas)
└── utils/         format.ts
```

**Estado global:** contexts por dominio; el carrito es **server-side** (persiste en BD por usuario); auth guarda JWT en `localStorage` (⚠️ a revisar en Fase 3).

**Patrón de datos:** página → `service.getX()` → `api.ts` (axios con Bearer) → backend. Loading/empty/error states uniformes en 31/32 páginas.

---

## 5. Modelo de datos (resumen)

Entidades principales y relaciones:

- **User** 1—N **Address**, 1—1 **Cart** (N—N **CartItem**→Product), N—N **Favorite**→Product, 1—N **Order**, 1—N **Review**, 1—N **SupportTicket**→N **TicketResponse**.
- **Product** N—1 **Category**, N—1 **Material**, 1—N **ProductImage**, 1—N **OrderItem**.
- **Order** 1—N **OrderItem**, 1—1 **Payment**; campos: `orderNumber` (ÚNICO), estado, subtotal/descuento/envío/total.
- **Coupon**: código único, activo, ventana de validez, `maxUses`, `minAmount`, `usesCount`.
- **PasswordResetToken**: hash SHA-256 (nunca token en claro), expiración 2 h.

Migraciones: `V1` (19 tablas) + `V2..V7` (alineaciones) + **segundo `V2` a renumerar en Fase 1**.

---

## 6. Flujos de negocio actuales

| Flujo | Estado real |
|---|---|
| Registro/Login (email+password) | ✅ Funcional (JWT, BCrypt, rate limit) |
| Login Google | ❌ Roto/inseguro: endpoint sin verificar + OAuth no configurado |
| Recuperar contraseña | ❌ No envía email (solo log con enlace) |
| Catálogo + búsqueda + filtros | ✅ Funcional con paginación (sin topes de size) |
| Carrito (server-side) | ✅ Funcional (falta validar stock acumulado) |
| Favoritos | ✅ Funcional |
| Checkout | ❌ Sin validación en cliente; saltos de paso; envía PAN |
| Pagos | ✅ Mercado Pago (Checkout Pro) + webhook firmado + sandbox en dev; solo la pasarela marca `PAID` |
| Pedidos + estados | ✅ Máquina de estados con devolución de stock + detalle admin con cliente/items |
| Cupones | ⚠️ Validación incompleta al crear pedido |
| Reseñas | ✅ Requieren compra previa; creación `PENDING` y moderación admin (aprobar/rechazar) |
| Soporte/tickets | ✅ Ownership verificado; hilo de respuestas admin↔cliente en ambos paneles |
| Panel admin | ✅ Dashboard con métricas reales + CRUDs (imágenes, pedidos, usuarios, soporte, reseñas) |
| Email transaccional | ❌ `EmailService` no envía nada |
| Cloudinary/imágenes | ❌ Config sin implementación Java |

---

## 7. Estado de verificación (2026-10-08)

| Check | Estado |
|---|---|
| `npm run lint` | ❌ 3 errores / 9 warnings |
| `npm run build` | ❌ 2 errores TypeScript (`Login.tsx`) |
| `mvn test` | ❌ no compila (JDK 17 < 25 requerido) |
| Backend arranque | ❌ bloqueado (Flyway V2 duplicado + YAML duplicado) |
| Tests backend | 13 unitarios (3 servicios); 0 de controllers/seguridad |
| CI/CD | ❌ no existe |
| Docker local | ❌ Docker no instalado en esta máquina |

---

## 8. Arquitectura objetivo (fases 1→12)

Se mantiene el **monolito modular Spring Boot** (sin microservicios/Kafka/K8s). Se evoluciona hacia:

```
Frontend (SPA, error boundaries, hooks, validación)
   ↓ REST (API versionada en contratos, errores JSON consistentes)
Controllers (solo entrada/salida, @Valid, sin lógica)
   ↓
Services (@Transactional, reglas de negocio, autorización de propiedad)
   ↓
Repositories (consultas paginadas, sin N+1, índices)
   ↓
PostgreSQL (Flyway V8+, índices, sin OSIV)
```

**Cambios estructurales previstos:**
- **Seguridad:** refresh tokens + rotación, logout real, cookies httpOnly (evaluar), Google OAuth verificado, rate limit confiable (Fase 3).
- **Pagos:** ✅ integración Mercado Pago real (preferencia de Checkout Pro + webhook con firma HMAC + idempotencia + reconciliación de montos); cero PAN en nuestros servidores; `SandboxPaymentProvider` simula la pasarela solo en dev (Fase 5).
- **Observabilidad:** `spring-boot-starter-actuator` (`/actuator/health`), logs estructurados sin DEBUG en prod (Fase 12).
- **CI/CD:** GitHub Actions (lint+tests+build por PR) (Fase 11).
- **Testing:** MockMvc, Testcontainers/H2+Flyway, Vitest, E2E Playwright (Fase 10).
- **Dependencias:** eliminar muertas (`oauth2-client`/`mail` si no se implementan, `three`), valorar SDK de Mercado Pago y upgrade a Spring Boot 4.x (Fase 12, decisión explícita).

**Principios:** SOLID/DRY/KISS sin sobreingeniería; seguridad > correctitud > mantenibilidad > UX > rendimiento > complejidad mínima; el backend es siempre la fuente de verdad de precios, stock y estados.
