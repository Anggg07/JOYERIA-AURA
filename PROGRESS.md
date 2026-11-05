# PROGRESS — Estado del proyecto (actualizado 2026-10-12)

Proyecto: modernización profesional del e-commerce de joyería (`joyeria-main-mobile`).
Plan maestro de 13 fases (FASE 0–12) siguiendo: explicar → modificar → tests/lint/build → documentar → notificar y **esperar autorización** antes de avanzar de fase.

Repo: `https://github.com/foxtrot0367/joyeria-main-mobile`
Estructura: `joyeria-backend` (Spring Boot 3.5 / Java 25 / PostgreSQL 17 / Flyway) y `joyeria-frontend` (React 18 + Vite 5 + TS + Tailwind).

---

## Cómo levantar el entorno

### PostgreSQL
```powershell
Get-Service postgresql-x64-17            # comprobar
Start-Service postgresql-x64-17          # arrancar si está detenido
```
BD `joyeria`, usuario `joyeria_user` / `joyeria_password`, puerto 5432. Flyway aplica V1→**V13** al arrancar.

### Backend (`joyeria-backend`)
```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:JWT_SECRET = [Convert]::ToBase64String([byte[]](1..48 | ForEach-Object { Get-Random -Maximum 256 }))
$env:SPRING_PROFILES_ACTIVE = 'dev'
mvn -o spring-boot:run
```
- Tests: `mvn -o test` (**119/119**). Cobertura JaCoCo en `target/site/jacoco/` (instrucciones ≈39 %, ramas ≈39 %; sin umbral mínimo).
- API en `http://localhost:8080`. Uploads en `/uploads`.

### Frontend (`joyeria-frontend`)
```powershell
npm run dev      # http://localhost:5173  (proxy /api y /uploads -> :8080)
npm run lint     # baseline: 0 errores, 10 warnings
npm run build    # tsc -b && vite build
npm test         # vitest run  (20/20)
npm run test:coverage   # vitest + cobertura v8
npm run test:e2e        # Playwright (requiere dev server + backend arriba)
```
Credenciales seed (perfil `dev`): `admin@joyeria.com` / `admin123` (ADMIN, id=1), `maria@example.com` / `user1234` (USER).

---

## Estado de fases

| Fase | Tema | Estado |
|------|------|--------|
| F0 | Reconocimiento / auditoría | ✅ |
| F1 | Base / seguridad auth | ✅ |
| F2 | Catálogo / productos | ✅ |
| F3 | Pedidos / carrito | ✅ (3.7 force-push pendiente, ver bloqueos) |
| F4 | Búsqueda / filtros | ✅ |
| F5 | Pagos (Mercado Pago + sandbox) | ✅ (E2E real bloqueado sin credenciales) |
| F6 | Panel admin | ✅ |
| F7 | UI/UX | ✅ commit `5fca33d` |
| F8 | Mobile | ✅ commit `3056e82` |
| F9 | Performance | ✅ commit `6615fe8` |
| F10 | Testing | ✅ (backend 119, frontend 20, E2E 1) |
| F11 | CI/CD | ✅ (workflow + branch protection doc) |
| F12 | Producción | ✅ (TLS, secrets, backups, actuator, docs) |

HEAD local: `d7666d8` (F10) + `4fc1575` (F11) + commit de F12 (Producción).
Contadores: 88 endpoints (`TOTAL_MAPPINGS=88`, 21 controllers).

---

## FASE 9 — Performance ✅ (commit `6615fe8`)

### Cambios aplicados
Backend:
- `application.yml`: `spring.jpa.open-in-view: false` (B-M4) + `hibernate.default_batch_fetch_size: 30` (mitigación global de N+1).
- Transacciones de lectura `@Transactional(readOnly = true)` a nivel de clase:
  - `ProductService`, `OrderService`, `FavoriteService`, `SupportService`, `CategoryService`, `ReviewService`.
  - `CartService`: readOnly en clase + `@Transactional` (read-write) en `getCart` (hace `save()` de carrito vacío).
  - `ProductService.deleteProduct` → `@Transactional`.
  - `CategoryService`/`AddressService`/`ReviewService`: `@Transactional` en métodos de escritura.
  - `PaymentService.getPaymentStatus` → `@Transactional(readOnly = true)`.
- `ProductRepository`: nuevo `countActiveGroupedByCategory()` (agrega conteo por categoría).
- `CategoryService.getAllCategories()` usa el conteo agrupado → **elimina el N+1** de counts.
- **NUEVO** `db/migration/V13__favorite_fk_indexes.sql`: índices faltantes (B-B6)
  `support_tickets.user_id`, `ticket_responses.ticket_id`, `reviews.user_id` y UNIQUE `reviews(user_id, product_id)` (con dedupe previo).

Frontend:
- `ProductCard.tsx`: envuelto en `memo`.
- `Catalog.tsx`: `handleAddToCart` con `useCallback`; `onQuickView={setQuickView}`; **debounce 400 ms** en inputs de precio (Mín/Máx) usando `setSearchParams` funcional; `clearFilters` resetea el estado local.
- `AuthContext.tsx` y `CartContext.tsx`: callbacks con `useCallback` + value del provider con `useMemo`.
- `vite.config.ts`: ya tenía `manualChunks` (react/motion/icons) y `chunkSizeWarningLimit: 900`.

### Verificación
- `mvn -o test`: **95/95** ✅.
- Arranque real con OSIV off (perfil dev) contra PostgreSQL: OK, Flyway migró a **v13** ✅.
- Smoke E2E completo (`smoke6.ps1`): login admin, dashboard stats, upload, crear producto, compra + pago sandbox, detalle pedido admin, transiciones de estado, protección último admin, tickets de soporte (+respuesta admin, vista cliente), reseñas + moderación — **todo OK** ✅.
- Pruebas extra (rutas lazy bajo OSIV off): `/api/categories` (con `productCount`), `/api/admin/orders` (con items), `/api/favorites`, `/api/cart` → **200** ✅. Sin `LazyInitializationException`.
- Frontend: `npm run lint` → 0 errores / 10 warnings; `npm run build` → OK con chunks separados (react 162 kB, motion 122 kB, icons 30 kB, index 98 kB) ✅.

### Pendiente
1. Confirmar la prueba de favoritos con imágenes (chequeo de script, no del server).

---

## FASE 10 — Testing ✅

### Backend
- `pom.xml`: plugin **JaCoCo `0.8.15`** (`prepare-agent` + `report` en fase `test`; compatible con JDK 25). Reporte en `joyeria-backend/target/site/jacoco/`.
- 4 clases de test **MockMvc standalone** en `com.joyeria.controller` (sin contexto Spring; `MockMvcBuilders.standaloneSetup(...).setControllerAdvice(new GlobalExceptionHandler())`): `OrderControllerTest` (7, incl. ownership y rol admin), `AuthControllerTest` (5), `ProductControllerTest` (6), `PaymentControllerTest` (6) → **24 tests nuevos**.
- Total backend: **119/119** ✅. Cobertura: instrucciones **39.2 %**, ramas **38.6 %** (no se fija umbral mínimo para no romper la build).

### Frontend
- Deps: `vitest@3.2.7` (Vitest 5 exige Vite 6; el proyecto usa Vite 5.4), `@vitest/coverage-v8`, `jsdom`, `@testing-library/react|jest-dom|user-event`.
- `vitest.config.ts` (NUEVO): entorno jsdom, `setupFiles: ./src/test/setup.ts`, `exclude` incluye `e2e/**` (para que el spec de Playwright no lo recoja Vitest), cobertura v8.
- `src/test/setup.ts` (NUEVO): `@testing-library/jest-dom/vitest`, `afterEach(cleanup)` (no se usa `globals`), polyfills `matchMedia`/`IntersectionObserver`/`ResizeObserver` (framer-motion).
- 4 archivos de test (**20/20**): `src/utils/errors.test.ts` (5), `src/services/auth.service.test.ts` (5), `src/contexts/CartContext.test.tsx` (5), `src/components/ProductCard.test.tsx` (5).
- Scripts en `package.json`: `test`, `test:coverage`, `test:e2e`.

### E2E (Playwright)
- `@playwright/test` + Chromium instalado. `playwright.config.ts` (NUEVO) con `baseURL http://localhost:5173` (no gestiona webServer; requiere dev+backend vivos).
- `e2e/purchase-flow.spec.ts` (NUEVO): camino feliz registro → catálogo → carrito → checkout (envío) → pago → pasarela sandbox → `/pedido-confirmado`. `context.addInitScript` precarga el consentimiento de cookies.
- `.gitignore`: añadidos `playwright-report/`, `test-results/`, `blob-report/`, `playwright/.cache/`. `eslint.config.js`: `ignores` incluye esos dirs + `coverage`.

### Verificación
- `mvn test` **119/119** ✅ · `npm test` **20/20** ✅ · `npm run test:e2e` **1/1** ✅ · `npm run lint` 0 errores / 10 warnings · `npm run build` OK.

### Pendiente
1. Notificar al usuario y **pedir autorización para FASE 11 — CI/CD**.

---

## FASE 11 — CI/CD ✅

- **NUEVO** `.github/workflows/ci.yml` (dispara en `push`/`pull_request` a `main`; `concurrency` con cancelación; `permissions: contents: read`):
  - **backend** (JDK 25 temurin + cache Maven): `mvn -B -ntp verify` (119 tests + jar) + artefacto JaCoCo.
  - **frontend** (Node 20 + cache npm): `npm ci` → `npm run lint` → `npm run test:coverage` → `npm run build` + artefacto de cobertura.
  - **docker** (solo `push`, `needs: [backend, frontend]`): build de las imágenes de backend y frontend (no publica).
- **Branch protection documentado** en `README.md` (§ Integración continua): PR obligatorio, checks requeridos `Backend (Java 25 / Maven)` y `Frontend (Node 20 / Vite)`, ramas al día; aprobaciones opcionales. Requiere admin del repo.
- (Opcional) `dependency-review` documentado como mejora futura.
- **Verificación local de los comandos del workflow:** `mvn -B -ntp verify` → BUILD SUCCESS (119 tests + jar); `npm ci` OK (lockfile sincronizado); `npm run lint` 0/10; `npm run test:coverage` 20/20; `npm run build` OK. Tags de los `Dockerfile` verificados en Docker Hub.
- **Nota entorno:** `npm ci` local falla si el dev server de Vite está vivo (bloquea `node_modules`); detenerlo primero. Sin `@SpringBootTest`, el CI no necesita PostgreSQL.

### Pendiente
1. Notificar al usuario y **pedir autorización para FASE 12 — Producción**.

---

## FASE 12 — Producción ✅

- **Actuator/health**: `spring-boot-starter-actuator`; solo se expone `health` (+ probes `liveness`/`readiness`, `show-details: never`). `SecurityConfig` permite `/actuator/health/**`. El `MailHealthIndicator` se activa solo con `EMAIL_ENABLED` (`management.health.mail.enabled: ${EMAIL_ENABLED:false}`) para no marcar DOWN cuando el correo es opcional.
- **Compose**: `docker-compose.yml` prod-safe (solo Caddy publica `80`/`443` con `--profile tls`; PostgreSQL/backend/frontend internos, `restart: unless-stopped`, healthchecks, red dedicada, secretos `${VAR:?}`). **NUEVO** `docker-compose.dev.yml` (publica `5173`/`8080`/`5432`, perfil `dev`) y **NUEVO** `Caddyfile` (TLS automático para `DOMAIN`).
- **Volumen `uploads`** (`uploads:/app/uploads`); `Dockerfile` backend pre-crea `/app/uploads` con propietario `joyeria` (contenedor no-root). HEALTHCHECK del `Dockerfile` corregido a `/actuator/health`.
- **Logs**: **NUEVO** `application-prod.yml` (root INFO, Spring/Hibernate WARN, patrón de consola).
- **Backups**: **NUEVOS** `scripts/backup-db.sh` (pg_dump + retención 30 días) y `scripts/restore-db.sh`.
- **Secretos (C6)**: `JWT_SECRET` obligatorio sin default; rotación documentada. **README** ampliado (despliegue, operaciones, variables), **LICENSE** MIT, **`.env.example`** con `DOMAIN`/`ACME_EMAIL`/`TRUSTED_PROXIES`/`LOG_LEVEL`, **`.gitattributes`** (LF para scripts).
- **Decisión Spring Boot 4.x**: documentada en README (se permanece en 3.5.x; migración como iniciativa aparte).
- **Verificación:** `mvn test` 119/119; arranque real en perfiles `dev` y `prod` → `/actuator/health` 200 `{"status":"UP","groups":["liveness","readiness"]}`; `/actuator/env` y `/actuator/beans` no expuestos; `/api/products` 200. Compose/override validados como YAML. Docker no instalado localmente → `docker build`/`compose up` se validan en CI/host.

### Pendiente
1. Notificar al usuario: **plan de 13 fases (FASE 0–12) completado** — cierre del proyecto.

### Fase IA — Smart Gift Finder ✅

- **FASE 0 (auditoría):** `docs/GIFT_FINDER_AUDIT.md` (decisión **Opción D**, sin
  cambios de esquema).
- **Backend:** paquete `com.joyeria.gift` (`GiftFinderController` →
  `POST /api/gift-finder/recommend` público; `GiftFinderService` con scoring
  determinista, consulta real y explicaciones; `dto/**`). Reutiliza
  `TextNormalizer`, `ProductRepository.searchFiltered` y `ProductService.mapProducts`.
  Integración en `SecurityConfig` (permitAll) y `RateLimitFilter` (20/60 s por IP).
- **Frontend:** `services/gift.service.ts`, `pages/GiftFinder.tsx` (asistente de 5
  pasos + "No estoy seguro", URL compartible, resultados reales + agregar al
  carrito), ruta en `App.tsx`, enlace en `Navbar.tsx`, tipos en `types/index.ts`.
- **Principio:** el motor solo ordena/explica; los datos comerciales provienen
  siempre de PostgreSQL. Presupuesto siempre respetado; disponibilidad siempre
  evaluada; relevancia nullable sin criterios.
- **Docs:** `docs/GIFT_FINDER.md`.
- **Verificación:** `mvn -o test` **159/159** ✅ (15 nuevos); `npm run lint` 0
  errores; `npm test` **35/35** ✅ (9 nuevos); `npm run build` correcto.

### Fase IA — Asistente de compras (AURA) ✅

- **FASE 0 (auditoría):** `docs/AI_SHOPPING_ASSISTANT_AUDIT.md`.
- **Backend:** paquete `com.joyeria.ai` (`AiAssistantController` → `POST /api/ai/assistant`
  público; `AiAssistantService` con fallback; `SearchIntentParser` determinista;
  `ProductSearchService` sobre PostgreSQL; `ProductRecommendationService` con scoring;
  `AiPromptService`; `client/**` compatible OpenAI vía `RestClient`; `dto/**`).
  Integración en `ProductService.mapProducts`, `SecurityConfig` (permitAll),
  `RateLimitFilter` (20/60 s por IP), `application.yml` (bloque `ai:`) y `.env.example`.
- **Frontend:** `services/ai.service.ts`, `components/AiAssistantWidget.tsx`
  (chat + sugerencias reales + agregar al carrito), montado en `MainLayout.tsx`;
  tipos y `vite-env.d.ts`.
- **Principio:** la IA solo interpreta intención y redacta; los datos comerciales
  provienen siempre de PostgreSQL. `AI_ENABLED=false` por defecto (modo determinista).
- **Docs:** `docs/AI_SHOPPING_ASSISTANT.md`.
- **Verificación:** `mvn -o test` **144/144** ✅ (25 nuevos); `npm run lint` 0 errores;
  `npm test` **26/26** ✅; `npm run build` correcto.

---

## Bloqueos / notas abiertas

- **Force-push FASE 3.7 (secreto JWT)**: `git push --force origin main` → 403; la cuenta `gh` activa es `sixzyy` y **no tiene permiso de escritura** en `foxtrot0367/joyeria-main-mobile`. El remoto aún contiene el secreto JWT (tratado como comprometido). Opciones: reautenticar como `foxtrot0367`, añadir `sixzyy` como colaborador, o push desde otra máquina.
- **E2E real de pagos**: bloqueado sin `MERCADOPAGO_ACCESS_TOKEN` real + URL pública (mitigado con `SandboxPaymentProvider` en dev).
- **Verificación manual móvil 360/375/390/430 px (F8)**: no ejecutable sin navegador (guardas automáticas ya aplicadas).
- **`Dockerfile` backend — HEALTHCHECK**: corregido en FASE 12 a `/actuator/health` (con `spring-boot-starter-actuator`). ✅

---

## Lecciones aprendidas / convenciones

- **NO usar `Get-Content -Raw` + `Set-Content`** para editar: corrompe acentos UTF-8 (BOM + mojibake). Usar siempre las herramientas de edición del CLI. (Ocurrió en `Navbar.tsx`, restaurado con `git checkout`.)
- PowerShell 5.1: no existe `Invoke-RestMethod -Form` (usar `curl.exe` para multipart); `$pid` es reservada (usar `$prodId`); `-Dtest=A,B` requiere comillas simples.
- El shell wrapper reporta `ChildProcess.kill` al lanzar procesos en segundo plano con `Start-Process`, pero el proceso **sí queda vivo**.
- Vite dev server corriendo en **5173**; backend en **8080**.

---

## Archivos clave

- `ROADMAP.md`, `ARCHITECTURE.md`, `AUDIT.md` (registro estático, no se edita), `README.md`.
- Migraciones Flyway: `joyeria-backend/src/main/resources/db/migration/V1…V13`.
- Script de smoke: `%TEMP%\opencode\smoke6.ps1`.
