# AUDIT.md — Auditoría del proyecto Joyería

**Fecha:** 2026-10-08
**Repositorio:** https://github.com/foxtrot0367/joyeria-main-mobile (`main` @ `bdbef47`)
**Alcance:** Frontend, backend, base de datos, seguridad, Docker, documentación.
**Método:** Lectura estática completa del código + verificación dinámica (instalación, lint, build, tests).
**Regla aplicada:** Fase 0 del prompt maestro — este documento no modifica código.

---

## 1. Resumen ejecutivo

El proyecto tiene una **base de ingeniería razonable y mejor de lo esperado**: arquitectura por capas convencional y bien nombrada en backend, frontend con TypeScript strict, servicios API centralizados, code splitting real, Docker multi-stage con usuario no-root y ausencia de secretos en el árbol actual del working tree.

Sin embargo, **no está en estado desplegable ni siquiera de desarrollo** por tres bloqueos:

1. **El backend no arranca**: dos migraciones Flyway con versión `V2` duplicada + clave raíz `spring:` duplicada en `application.yml`.
2. **El frontend no compila**: 2 errores TypeScript en `Login.tsx` y 3 errores de lint.
3. **Autenticación comprometida**: `POST /api/auth/google` emite JWT (con rol ADMIN si el email coincide) sin verificar ninguna credencial.

Además existen fallos graves de negocio y seguridad: pagos falsos con IDOR, credenciales de admin sembradas en cualquier entorno, checkout de frontend sin validación que permite "confirmar" pedidos sin pagar, y envío de datos de tarjeta al backend propio (riesgo PCI).

**El backend tampoco puede compilarse en esta máquina**: requiere JDK 25 (`pom.xml`) y solo hay JDK 17 instalado (ver §2).

---

## 2. Estado de verificación dinámica (ejecutado el 2026-10-08)

| Verificación | Resultado | Detalle |
|---|---|---|
| `node --version` | ✅ v24.19.0 | npm 11.17.0 |
| `java --version` | ⚠️ JDK 17.0.20.1 | El proyecto requiere **JDK 25** (`pom.xml:21`) |
| `mvn --version` | ✅ Maven 3.9.16 | |
| `docker` | ❌ No instalado | No se pueden probar builds/compose localmente |
| `npm install` | ✅ OK | |
| `npm run lint` | ❌ **3 errores, 9 warnings** | Errores en `Login.tsx` (`authService` y 2× `err` sin usar) |
| `npm run build` (`tsc -b && vite build`) | ❌ **FALLA** | `Login.tsx(49,9)` y `Login.tsx(76,9)`: `'window.google' is possibly 'undefined'` (TS18048) |
| `mvn test` | ❌ **FALLA** | `Fatal error compiling: release version 25 not supported` (JDK 17 < 25) |

**Consecuencia:** ningún build del proyecto pasa hoy. La Fase 1 debe dejar `lint` y `build` en verde antes de cualquier otra cosa. Para backend será necesario instalar JDK 25 (o bajar `java.version` a un LTS soportado localmente — decisión a tomar en Fase 1).

---

## 3. Hallazgos CRÍTICOS

> Bloquean el arranque, comprometen la seguridad o rompen el flujo principal de negocio.

### C1 — Backend no arranca: migraciones Flyway `V2` duplicadas
- **Archivos:** `joyeria-backend/src/main/resources/db/migration/V2__add_google_id_to_users.sql` y `V2__align_cart_table_with_entity.sql`
- **Problema:** dos migraciones con versión 2 → Flyway aborta con `DUPLICATE_VERSIONED_MIGRATION`. Ambas son necesarias (una renombra `carts`→`cart`, otra añade `google_id`), ninguna se puede descartar.
- **Impacto:** arranque imposible (`spring.flyway.enabled: true`); `docker-compose` deja el backend unhealthy.
- **Corrección:** renumerar **una sola** a `V8__...` (nunca editar una migración ya aplicada en otros entornos). No hay más versiones duplicadas (V1, V3–V7 únicas — verificado).

### C2 — Backend no arranca: clave raíz `spring:` duplicada en `application.yml`
- **Archivo:** `joyeria-backend/src/main/resources/application.yml` (líneas 4 y 37)
- **Problema:** dos claves `spring:` de nivel superior (datasource/jpa/flyway vs security.oauth2). Spring fija `allowDuplicateKeys=false` → excepción de carga, o *last-wins* descartando datasource/Flyway/JPA.
- **Impacto:** arranque imposible o pérdida silenciosa de la configuración de BD. `application.example.yml` no tiene el problema (solo el archivo real).
- **Corrección:** fusionar en un único bloque `spring:`.

### C3 — Bypass total de autenticación en `POST /api/auth/google`
- **Archivos:** `AuthController.java:67`, `AuthService.loginWithGoogle`, `SecurityConfig.java:60` (`/api/auth/**` en `permitAll`)
- **Problema:** el endpoint recibe `email`, `googleId`, `firstName`, `lastName` **del cliente** y devuelve un JWT válido sin verificar ningún token de Google. Si el email coincide con `admin@joyeria.com`, el JWT sale **con rol ADMIN**.
- **Impacto:** toma de control total de la API por cualquier anónimo. Además `GET /api/auth/google/callback` no funciona (no hay `.oauth2Login(...)`) → 500.

### C4 — Pagos falsos + IDOR en `POST /api/payments/process`
- **Archivos:** `PaymentService.java`, `PaymentController.java`, `dto/PaymentRequest.java`
- **Problema:** no recibe `Authentication`, no verifica propiedad del pedido, ni estado, ni monto; no contacta con Mercado Pago (`provider="sandbox"` fijo, referencia aleatoria) y marca `Payment.APPROVED` + `Order.PAID` incondicionalmente; sin idempotencia. `GET /api/payments/reference/{ref}` es IDOR de lectura.
- **Impacto:** cualquier usuario autenticado puede marcar como pagado cualquier pedido ajeno. `PaymentRequest` además acepta `cardNumber`/`token` que nunca se usan (superficie PCI innecesaria).

### C5 — Credenciales de admin sembradas en cualquier entorno + logueadas
- **Archivo:** `config/DataInitializer.java:92-114`
- **Problema:** `CommandLineRunner` sin `@Profile` ni `@ConditionalOnProperty` que crea `admin@joyeria.com` / `admin123` y `maria@example.com` / `user1234` si la tabla está vacía, y hace `log.info` con la contraseña.
- **Impacto:** en cualquier despliegue nuevo con BD limpia queda un admin de credencial conocida, registrada en logs.

### C6 — Secreto JWT en el historial de git con repositorio público
- **Archivo histórico:** `application.yml` en commit `07b2c68` (línea 29, valor base64 con default). Eliminado en `2d38870`.
- **Problema:** el repo es público; el valor histórico es recuperable por cualquiera mientras siga en uso en algún despliegue.
- **Impacto:** firma de JWT válidos por terceros.
- **Acción:** rotación obligatoria del secreto + limpieza de historial (`git filter-repo`) — requiere coordinación (puede romper clones existentes).

### C7 — Checkout del frontend roto (sin validación, confirmación falsa)
- **Archivo:** `joyeria-frontend/src/pages/Checkout.tsx`
- **Problemas:**
  - No hay `<form>`: los `required` son inertes; se avanza con campos vacíos.
  - `handlePay` no valida nada en cliente antes de crear el pedido.
  - Los indicadores de paso permiten saltar al paso 3 ("¡Gracias por tu compra!") **sin crear ni pagar ningún pedido**.
  - El número de tarjeta (`cardNumber`, `cvv`, `expiry`, `cardName`) se envía al backend propio en `POST /payments/process` mientras la UI afirma "No almacenamos datos de tarjetas" → **riesgo PCI-DSS**.
  - Toast de error duplica el mensaje ("Error: msg msg").
- **Impacto:** el flujo de conversión principal está roto; pedidos con datos vacíos y pantalla de confirmación falsa.

### C8 — Frontend no compila ni pasa lint
- **Archivos:** `src/pages/Login.tsx`
- **Problemas:** 2 errores TS18048 (`window.google` posiblemente `undefined`, líneas 49 y 76) y 3 errores `@typescript-eslint/no-unused-vars` (`authService`, 2× `err`).
- **Impacto:** `npm run build` y `npm run lint` fallan → ningún CI podría pasar.

---

## 4. Hallazgos ALTOS

### Backend
| ID | Hallazgo | Archivos |
|---|---|---|
| B-A1 | OAuth Google no funcional: sin `.oauth2Login(...)`, starter oauth2-client sin uso, callback → NPE → 500 | `SecurityConfig.java`, `AuthController.java:77-83`, `pom.xml:42` |
| B-A2 | Usuarios Google nuevos no creables: `password=""`/`phone=""` chocan con `@Size(min=8)`/`@NotBlank` + `phone` UNIQUE en BD | `User.java:46-54`, `AuthService.java`, `GoogleOAuthService.java` |
| B-A3 | IDOR en tickets de soporte (lectura y escritura sin ownership) | `SupportController.java`, `SupportService.java` |
| B-A4 | Recuperación de contraseña no envía email: solo `log.warn` con el enlace; con log DEBUG en prod, el enlace queda en logs | `EmailService.java`, `application.yml:69-72` |
| B-A5 | Reseñas auto-aprobadas sin verificar compra | `ReviewService.java` |
| B-A6 | Sin control de concurrencia en stock (sin `@Version` ni bloqueo) → sobreventa | `OrderService.java:78-92` |
| B-A7 | Cupones no validados al crear pedido (`validateCouponForOrder` es código muerto); incrementa `usesCount` aunque se supere el límite | `OrderService.java:95-112`, `CouponService.java:44` |
| B-A8 | Spring Boot 3.5.16 en fin de soporte OSS (2026-06-30) | `pom.xml:10` |
| B-A9 | Healthcheck del Dockerfile apunta a `/api/health` inexistente; sin actuator | `joyeria-backend/Dockerfile:16` |
| B-A10 | CORS hardcodeado a `localhost:5173/3000` ignorando `FRONTEND_URL`; `allowCredentials(true)` | `SecurityConfig.java:82-96` |

### Frontend
| ID | Hallazgo | Archivos |
|---|---|---|
| F-A1 | Login Google inoperativo: `VITE_API_URL` no inyectada en build Docker (runtime ≠ build), posible doble prefijo `/api/api/...`, `VITE_GOOGLE_CLIENT_ID` sin declarar en `vite-env.d.ts` | `Login.tsx:53`, `Dockerfile`, `docker-compose.yml:45` |
| F-A2 | Cualquier 401 dispara `window.location.href = '/login'` (recarga total, sin mensaje); invitado que agrega al carrito desde Catalog/Home/Personalize es redirigido sin aviso | `services/api.ts:26-28` |
| F-A3 | Sin `ErrorBoundary` en toda la app (0 resultados) → error de render = pantalla en blanco | `App.tsx` |
| F-A4 | Token JWT en `localStorage` sin cabeceras de seguridad en nginx (sin CSP/HSTS/X-Frame-Options); dato `user` escrito y nunca leído | `api.ts:16`, `nginx.conf`, `AuthContext.tsx:60` |
| F-A5 | Admin inusable en móvil: sidebar `w-64` sin variantes `md:` ni hamburguesa; `/admin/*` sin clases responsive | `AdminLayout.tsx` |

### Infraestructura
| ID | Hallazgo | Archivos |
|---|---|---|
| I-A1 | PostgreSQL publicado en host (`5432:5432`) con contraseña por defecto `joyeria_password` | `docker-compose.yml:9,12-13` |
| I-A2 | API backend publicada directa en 8080 (sin pasar por nginx) | `docker-compose.yml:30-31` |
| I-A3 | Todo el tráfico en HTTP plano (sin TLS) | `docker-compose.yml:47`, `nginx.conf:2` |
| I-A4 | Rate limiting anulable por `X-Forwarded-For` spoofable (y estado en memoria) | `RateLimitFilter.java:82-92` |
| I-A5 | Variables de `.env.example` que nunca llegan al contenedor (`GOOGLE_*`, `EMAIL_*`, `MERCADOPAGO_*`, `CLOUDINARY_*`, `UPLOAD_DIR`) → esas features no funcionan en Docker | `docker-compose.yml:22-29` |
| I-A6 | Sin CI/CD (no existe `.github/`); Dockerfile build con `-DskipTests` | raíz |
| I-A7 | Incompatibilidad entorno: `java.version=25` vs JDK 17 local (verificado: build backend falla) | `pom.xml:21` |

---

## 5. Hallazgos MEDIOS

### Backend
| ID | Hallazgo | Archivos |
|---|---|---|
| B-M1 | Rate limit: regla `GET\|/api/coupons/validate` no coincide con el endpoint real `POST /validate`; estado en memoria | `RateLimitFilter.java:24-31` |
| B-M2 | N+1 real en `OrderService.mapToDTO`, `FavoriteService`, `CategoryService` (sin `@Transactional`/`JOIN FETCH`) | servicios citados |
| B-M3 | `LIKE '%q%'` no indexable; `size` sin tope (DoS); `sort` del cliente → 500 por `PropertyReferenceException` | `ProductRepository.java`, `ProductController.java`, `SearchController.java` |
| B-M4 | OSIV activo por defecto (sin `spring.jpa.open-in-view: false`) → conexión retenida por request | `application.yml` |
| B-M5 | `GlobalExceptionHandler` incompleto: cast ciego a `FieldError` (ClassCastException → 500); sin 405/415; 401/403 de Security no generan JSON | `GlobalExceptionHandler.java` |
| B-M6 | Soporte incompleto: `SupportTicketDTO` sin `responses`; sin endpoint admin para responder; ruta `permitAll /support/tickets/guest` huérfana (404) | `SupportTicketDTO.java`, `SupportController.java` |
| B-M7 | Sin `@Valid` en 8 PUT (addresses, admin/products, categories, materials, coupons, social-links, users/me, auth/google) | múltiples controllers |
| B-M8 | `GET /api/auth/me` y `GET /api/reviews/check/{id}` sin null-check de `Authentication` → 500 en vez de 401 | `AuthController.java:54`, `ReviewController.java:41` |
| B-M9 | Identificadores débiles: `orderNumber`/`ticketNumber` con 8 hex (32 bits) UNIQUE y consultables públicamente | `OrderService.java`, `SupportService.java` |
| B-M10 | `flyway.baseline-on-migrate: true` sin `baseline-version` → omite V1 en BD heredadas | `application.yml:20-23` |
| B-M11 | Dashboard suma totales de TODOS los pedidos (incluye CANCELLED/PENDING) → ingresos falsos | `OrderService.java:214-224` |
| B-M12 | Sin revocación de tokens: sin logout, refresh, `jti`; cambio de contraseña no invalida JWT vigentes; `loadUserByUsername` en cada request | `JwtTokenProvider.java`, `JwtAuthenticationFilter.java` |
| B-M13 | Config muerta en `application.yml` (payment MP, cloudinary, multipart, oauth2 client) sin código Java que la use | `application.yml` |
| B-M14 | Código muerto: `SearchRequest.java`, `validateCouponForOrder`, `createGuestTicket`, `Cart.sessionId`, columnas huérfanas | varios |
| B-M15 | CORS duplicado: `@CrossOrigin` en 17/21 controllers vs `CorsConfigurationSource` de seguridad (comportamiento dispar) | `controller/*.java` |
| B-M16 | Newsletter: 400 con email inactivo (fuga) y `unsubscribe` anónimo sin validación | `NewsletterService.java` |
| B-M17 | `ddl-auto: validate` puede chocar con `V2` (`password DROP NOT NULL`) vs `@Column(nullable=false)`; `Category.description` 1000 vs VARCHAR(500); `Material.description` 2000 vs VARCHAR(500) | `User.java:53`, `V1`, entidades |

### Frontend
| ID | Hallazgo | Archivos |
|---|---|---|
| F-M1 | Carrito: parpadeo de "vacío" (ignora `loading`); sin `catch` en acciones; "−" no deshabilitado en cantidad 1 → envía `quantity: 0` | `CartPage.tsx`, `Checkout.tsx:62` |
| F-M2 | Rechazos de promesa sin `.catch` en Home (7 peticiones) y Catalog; estado de error confundido con "sin resultados" | `Home.tsx:71-88`, `Catalog.tsx:59-62` |
| F-M3 | Sin `AbortController` → carreras entre filtros/páginas | `Catalog.tsx:47-63`, `ProductDetail.tsx:30-42` |
| F-M4 | Lógica de envío mágica duplicada (`subtotal >= 500000 ? 0 : 15000`) | `CartPage.tsx:25`, `Checkout.tsx:53` |
| F-M5 | Bloque de extracción de errores duplicado 25 veces | 25 archivos en `src/pages` |
| F-M6 | Servicios duplicados: `adminService.getProducts/getOrders/updateOrderStatus` vs `productService`/`orderService`; `couponService` vive en `misc.service` | `admin.service.ts`, `misc.service.ts` |
| F-M7 | Newsletter del Home falsa (solo toast; `newsletterService.subscribe` existe y solo se usa en Footer) | `Home.tsx` |
| F-M8 | `onError` de imagen sin placeholder en ProductDetail/Checkout | `ProductDetail.tsx:83,92` |
| F-M9 | Datos fabricados en detalle: rating fijo "5.0" y `quantity` sin selector | `ProductDetail.tsx` |
| F-M10 | Sin `document.title` por ruta; sitemap desactualizado | `index.html`, `public/sitemap.xml` |
| F-M11 | Imagen hotlinkeada de `pse.com.co` en el paso de pago | `Checkout.tsx:143` |
| F-M12 | `/carrito` pública vs `/checkout` protegida: invitado no puede revisar carrito | `App.tsx:56-57` |
| F-M13 | Modal sin `role="dialog"`, foco atrapado ni cierre con Escape (salvo QuickViewModal) | `Modal.tsx` |
| F-M14 | 12 `<img>` sin `loading="lazy"`; consentimiento de cookies sin efecto real | varios, `CookieConsent.tsx` |

### Infraestructura
| ID | Hallazgo | Archivos |
|---|---|---|
| I-M1 | `nginx.conf` sin cabeceras de seguridad ni gzip | `joyeria-frontend/nginx.conf` |
| I-M2 | `VITE_*` como variables de runtime en compose (Vite las lee en build) | `docker-compose.yml:45`, frontend `Dockerfile` |
| I-M3 | Sin `.dockerignore` en backend; frontend incompleto (empaqueta `.env`, `.idea`) | `joyeria-backend/`, `.dockerignore` |
| I-M4 | `uploads/` sin volumen en compose y sin estar en `.gitignore` | `docker-compose.yml`, `.gitignore` |
| I-M5 | README incompleto: faltan variables (`GOOGLE_*`, `MERCADOPAGO_*`, `CLOUDINARY_*`…), credenciales seed, arquitectura, endpoints, despliegue | `README.md` |
| I-M6 | Sin LICENSE, sin `.editorconfig`/Prettier, sin `.github/` | raíz |
| I-M7 | `application.example.yml` residual y desactualizado (95% duplicado) | `joyeria-backend/src/main/resources/` |

---

## 6. Hallazgos BAJO

**Backend:** `AuthResponse.type` siempre `null` (Lombok `@Builder` sin `@Builder.Default`); admin puede desactivarse a sí mismo; `addItem` no valida stock acumulado del `CartItem`; `AddressCreateRequest.addressType` con default `SHIPPING` que resetea actualizaciones parciales; enumeración de usuarios en `register`; sin índices en `payments.provider_reference`, `support_tickets.user_id`, `ticket_responses.ticket_id`, `reviews.user_id` ni unique `(user_id,product_id)` en reviews; slug con `System.currentTimeMillis()` no determinista; encoding corrupto en comentario de `V2__add_google_id_to_users.sql`.

**Frontend:** `SearchBar.tsx` y `ProductTurntable.tsx` son código muerto (0 importaciones); `three`/`@types/three` sin uso real; `showPassword` declarado sin botón; `window.location.href='/'` tras login; `Math.random()` como key; `/productos?category=8` hardcodeado; `NotFound` fuera de `MainLayout` (404 sin navbar/footer); contextos con default `{} as T`; `tsconfig` no type-chequea `vite.config.ts` y `noUnusedLocals=false`; `@eslint/js` importado pero no declarado; `chunkSizeWarningLimit: 900` silencia avisos.

**Infra:** `version: '3.8'` obsoleto; sin `restart:` ni perfiles en compose; sin caché Maven en Dockerfile backend; frontend corre como root en nginx; `logging.level DEBUG` en prod; `RateLimitFilter` limpia con `now % 47 == 0`; sin healthcheck propio en servicio frontend de compose.

---

## 7. Lo que está bien hecho (NO romper)

**Backend**
1. Estructura de paquetes convencional y clara (`controller/dto/service/repository/model/security/config/exception`).
2. Ningún DTO expone `password`/secretos; `User.password` con `@JsonIgnore`.
3. `ApiResponse`/`PagedResponse` uniformes; paginación server-side en todos los listados.
4. Seguridad base bien diseñada: stateless, BCrypt, `@EnableMethodSecurity`, cabeceras HSTS/CSP/Permissions-Policy, doble barrera `@PreAuthorize` en admin, orden de filtros correcto.
5. `JwtTokenProvider` verifica firma antes de leer claims (JJWT 0.12).
6. Reset de contraseña bien diseñado a nivel de datos: token de 32 bytes, **hash SHA-256 almacenado**, expiración 2 h, respuesta uniforme sin enumeración.
7. Ownership verificado en direcciones y pedidos (404 en vez de 403 para no filtrar existencia).
8. Máquina de estados de pedidos con devolución de stock al cancelar.
9. Soft-delete en productos/categorías/materiales preservando histórico.
10. Flyway + `ddl-auto: validate` (nunca `update`); `V6` con patrón correcto de backfill.
11. Rate limiting con reglas específicas para login/registro/reset.
12. Docker backend multi-stage con usuario no-root.

**Frontend**
1. Capa de servicios 100% centralizada: cero `fetch`/`axios` fuera de `services` (única excepción: login Google).
2. Code splitting real: 32 páginas con `React.lazy` + chunks manuales.
3. TypeScript `strict: true`, cero `any` explícitos, tipos de dominio centralizados.
4. Guards correctos (`ProtectedRoute`, `AdminRoute`).
5. Sistema de diseño con tokens semánticos + modo oscuro + anti-FOUC.
6. `prefers-reduced-motion` respetado.
7. Empty states y skeletons uniformes en 31/32 páginas; `ToastContext` bien implementado.
8. Búsqueda con parámetros en URL (filtros compartibles).
9. 76 archivos / 7.383 líneas — promedio 97 líneas/archivo, saludable.
10. SEO base cuidado (meta, OG, robots con bloqueo de `/admin`).

**Infra**
1. Sin secretos reales en el working tree; `.env.example` con placeholders; `.gitignore` sólido con sección de secretos.
2. Dockerfiles multi-stage con tags fijos.
3. Healthchecks y `depends_on: service_healthy` correctos; volumen `postgres_data` persistente.

---

## 8. Inventario de endpoints (resumen)

81 endpoints verificados: **27 públicos**, **27 autenticados**, **27 ADMIN** (detalle completo en el análisis de fase; la auth deriva de `SecurityConfig` + `@PreAuthorize`).

- **Rutas `permitAll` sin endpoint implementado (404):** `/api/support/tickets/guest`, `/api/payments/webhook/**`.
- **Endpoints con problemas de autorización:** `POST /api/auth/google` (C3), `POST /api/payments/process` y `GET /api/payments/reference/{ref}` (C4), `GET/POST /api/support/tickets/{ticketNumber}...` (B-A3).
- **Doble barrera admin correcta:** `/api/admin/**` + `@PreAuthorize` por controlador.

---

## 9. Migraciones Flyway

```
V1__initial_schema.sql              (19 tablas, 8 índices)
V2__add_google_id_to_users.sql      ← 🔴 DUPLICADO (C1)
V2__align_cart_table_with_entity.sql← 🔴 DUPLICADO (C1)
V3__complete_entity_timestamps.sql  OK
V4__align_inventory_change_type.sql OK
V5__rename_password_reset_token_hash.sql OK
V6__align_payment_columns.sql       OK (buen patrón backfill)
V7__add_ticket_response_user.sql    OK
```
Único par duplicado: `V2`. **No editar V1–V7 ya aplicados** — renumerar a `V8`.

---

## 10. Tests existentes

- **Backend:** 13 tests unitarios Mockito en 3 clases (`AuthServiceTest` 5, `CouponServiceTest` 4, `OrderServiceTest` 4). Sin cobertura de controllers, seguridad, `PaymentService`, repositorios ni migraciones. Sin H2/Testcontainers → no hay `@SpringBootTest`. Sin JaCoCo.
- **Frontend:** sin tests (solo lint). Sin framework de tests instalado.
- **E2E:** no existe.

---

## 11. Prioridades consolidadas (Top 20 para Fase 1)

| # | ID | Prioridad | Esfuerzo |
|---|---|---|---|
| 1 | C1 Flyway V2 duplicado | Crítico | Trivial (renombrar) |
| 2 | C2 `spring:` duplicado en YAML | Crítico | Trivial (fusionar) |
| 3 | C8 Build/lint frontend en verde | Crítico | Bajo |
| 4 | I-A7 JDK 25 vs 17 (decisión entorno) | Crítico | Bajo |
| 5 | C3 Bypass `POST /api/auth/google` | Crítico | Medio |
| 6 | C5 DataInitializer sin perfil + log de credenciales | Crítico | Trivial |
| 7 | C7 Checkout: validación + bloqueo de saltos + quitar PAN | Crítico | Medio |
| 8 | C4 Pagos: ownership + no marcar pagado sin pasarela | Crítico | Medio |
| 9 | C6 Rotación JWT_SECRET + limpieza historial | Crítico | Coordinación |
| 10 | F-A2 401 → redirect duro | Alto | Bajo |
| 11 | F-A3 ErrorBoundary | Alto | Bajo |
| 12 | B-A4 Email reset real (o desactivar flujo) | Alto | Medio |
| 13 | B-A7 Validación de cupones en pedido | Alto | Bajo |
| 14 | B-A6 Concurrency de stock | Alto | Medio |
| 15 | B-A3 IDOR tickets de soporte | Alto | Bajo |
| 16 | B-A10 CORS con `FRONTEND_URL` | Alto | Bajo |
| 17 | I-A5 Inyección de variables en compose | Alto | Bajo |
| 18 | F-A1 Login Google build-time | Alto | Medio |
| 19 | I-A4 Rate limit X-Forwarded-For | Alto | Bajo |
| 20 | B-M7 `@Valid` faltantes en PUT | Medio | Bajo |

---

## 12. Riesgos de la modernización

1. **Contratos API**: 81 endpoints en uso por un frontend acoplado — cambios de respuesta deben migrarse en ambos lados en el mismo commit.
2. **Migraciones aplicadas**: nunca editar V1–V7; cualquier cambio de esquema es V8+.
3. **Historial git**: la limpieza del secreto JWT (C6) reescribe historia → obliga a force-pull en clones.
4. **Spring Boot 3.5 → 4.x** (B-A8): migración mayor; no hacerla en Fase 1, planificarla en fase de arquitectura/producción.
5. **Entorno local sin JDK 25 y sin Docker**: parte de la verificación (backend tests, compose) requiere instalar herramientas — requiere tu autorización.
6. **Pagos**: sin credenciales reales de Mercado Pago no se puede probar el flujo real (sandbox requiere cuenta/keys — informar, no inventar).
