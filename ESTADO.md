# Estado del proyecto — joyeria-main-mobile

## Resumen

- **Objetivo:** Mantener y mejorar el asistente IA **exclusivo del rol ADMIN** en el panel/dashboard (además del asistente de compras AURA), con acciones propuestas que el admin confirma.
- **Marca:** AURA. Tokens: `#F8F5EC` (fondo marfil), `#F3EFE6` (piedra), `#29251F` (texto carbón), `#71695F` (secundario), `#C9A227` (oro). Tipografías: Playfair Display + Inter.
- **Proyecto:** `C:\Users\Administrator\Documents\Default Project\joyeria-main-mobile`. Canónicos: `joyeria-frontend` y `joyeria-backend`. Ignorar `Default Project\joyeria-frontend` (obsoleto).
- **Entorno:** Windows PowerShell 5.1, Node v24.19.0, npm 11, Maven 3.9.16, JDK 25. Docker NO instalado. Sin `rg`/`mvnw` (usar `mvn`).
- **Remoto Git:** `sixzyy` (rama `main`). `foxtrot0367/main` bloqueado (historias no relacionadas). **NO commitear/pushear sin confirmación explícita**.

## Estado de servicios (actual)

- **PostgreSQL (postgresql-x64-17):** `Running` (StartType `Manual`). Puerto `5432` UP.
- **Backend joyeria (Spring Boot):** **DOWN** — detenido manualmente. Aplicación `com.joyeria.JoyeriaApplication`.
- **Frontend joyeria (Vite):** **DOWN** — detenido manualmente. Sirve en `http://localhost:5173` con proxy `/api` → `http://localhost:8080`.
- **VEXXA-STUDIO:** detenido (liberó 8080/5173).

## Estado del código

### Backend (IA Admin Assistant) — COMPLETO

- Paquete `com.joyeria.ai.admin.*`: `AiAdminAssistantService`, `AiAdminPromptService`, `AdminInsightsService`, `AiAdminController`, DTOs, cliente HTTP compatible OpenAI.
- `AiProperties.java` (config IA/Admin). `OpenAiChatHttp.java`: fix header `Authorization: Bearer <apiKey>` (commit `a3b2d26`).
- `OpenAiCompatibleAdminChatClient.java`: pasa `apiKey` correctamente.
- Repositorios: `OrderRepository.findTop5ByOrderByCreatedAtDesc()`, `ReviewRepository.countByStatus`, `SupportTicketRepository.countByStatusIn`, `ProductRepository` (soporte insights).
- `application.yml`: configuración `ai.admin`.
- Seguridad: `RateLimitFilter` regla `/api/admin/ai/assistant` (40 req/60s).
- Seed: `DataInitializer.java` (admin `admin@joyeria.com`/`admin123`, user `maria@example.com`/`user1234`).

### Frontend (IA Admin Assistant) — COMPLETO

- Tipos: `src/types/index.ts` (`AiAdminActionType`, `AiAdminAction`, `AiAdminResponse`).
- Servicio: `src/services/aiAdmin.service.ts` (`ask(message, history)`, flag `aiAdminEnabled` desde `VITE_AI_ADMIN_ENABLED`).
- Componente: `src/components/admin/AdminAiAssistant.tsx` (widget flotante + confirmación de acciones). Ejecuta acciones vía `orderService.updateStatus`, `adminService.toggleUserActive`, `adminService.moderateReview`.
- Integración: `src/layouts/AdminLayout.tsx` monta `<AdminAiAssistant/>`; corrección `Joyeria` → `Joyería`.
- Env: `src/vite-env.d.ts` declara `VITE_AI_ADMIN_ENABLED`. `.env`/`.env.example` con `VITE_AI_ADMIN_ENABLED=true`.

### Optimizaciones IA Dashboard

- `AdminInsights`: añade `weekRevenue`, `pendingReviews`, `openTickets`, `recentOrders` (5 últimos con ID real).
- Prompt con contrato de acciones + parámetros exactos (`UPDATE_ORDER_STATUS {orderId, status}`, `TOGGLE_USER_ACTIVE {userId}`, `MODERATE_REVIEW {reviewId, status}`).
- Fallbacks ampliados (reseñas, tickets, semana, resumen).

### Tests

- **Backend:** **218/218 tests OK** (`AdminInsightsServiceTest`, `AiAdminAssistantServiceTest`, `AiAdminControllerTest` + existentes).
- **Frontend:** `npm run lint` **0 errores, 10 warnings** (preexistentes). `npm run build` **OK**. `npm test` **43/43 OK**.

## Cambios recientes (responsive móvil) — 16/10/2026

Ajustes aplicados en `joyeria-frontend` para mejorar experiencia ≤ 640px (sin romper desktop):

| Archivo | Cambio | Línea(s) |
|---|---|---|
| `src/components/QuickViewModal.tsx` | Miniaturas con `flex-wrap` (evita corte en móvil) | 77 |
| `src/pages/Login.tsx` | Padding lateral `px-4` en wrapper | 38 |
| `src/pages/Register.tsx` | Padding lateral `px-4`; grids `grid-cols-1 sm:grid-cols-2` (Nombre/Apellido, Contraseña/Confirmar) | 42, 53, 83 |
| `src/pages/ForgotPassword.tsx` | Padding lateral `px-4` | 33 |
| `src/pages/ResetPassword.tsx` | Padding lateral `px-4` | 40 |
| `src/pages/user/Profile.tsx` | Grid Nombre/Apellido `grid-cols-1 sm:grid-cols-2` | 65 |
| `src/pages/user/Addresses.tsx` | Header con `flex-wrap gap-3`; grids modal `grid-cols-1 sm:grid-cols-2` | 101, 140, 152, 158 |
| `src/components/OrderTimeline.tsx` | Scroll horizontal en móvil (`overflow-x-auto`, `min-w-[520px]`) | 24–50 |
| `src/contexts/ToastContext.tsx` | Ancho responsivo `w-[min(24rem,calc(100vw-2rem))]` | 35 |
| `src/pages/OrderConfirmation.tsx` | Botones con `flex-wrap` | 143 |
| `src/pages/Personalize.tsx` | Encabezado paso 1 apilado en móvil (`flex-col sm:flex-row`) | 198 |
| `src/components/Button.tsx` | Tamaño `sm` con `min-h-11` (tap target ≥44px) | 19 |
| `src/components/Pagination.tsx` | Botones numéricos `w-11 h-11` | 18 |
| `src/pages/Home.tsx` | Botón newsletter con `min-h-11` | 451 |
| `src/components/Footer.tsx` | Inputs/botones newsletter con `min-h-11` | 74–75 |

**Validación:** `npm run lint` 0 errores, `npm run build` OK.

## Commits relevantes (push a sixzyy/main)

- `f9febe1` — Branding AURA + home/estilos
- `589a916` — Asistente IA exclusivo ADMIN (backend+frontend + tests)
- `a3b2d26` — Fix Authorization header en OpenAiChatHttp
- `e2a95d7` — Mejora AdminInsights (semana, reseñas, tickets, últimos pedidos)

## Notas importantes

- **Variable de entorno IA:** `.env` contiene `AI_API_KEY=gsk_...` **expuesta**. Requiere rotación manual en consola de Groq (no puede hacerlo el agente).
- **StartType PostgreSQL:** `Manual`. Al reiniciar backend tras apagado, PostgreSQL puede requerirse iniciar (ya verificado y funcionando).
- **Patrón de contraste:** Evitar tokens con tema (`bg-ink`, `text-background`) en superficies sólidas; usar hex fijos (`bg-[#29251F]`, `text-[#F8F5EC]`).
- **PowerShell 5.1:** No usar `$port:` (usar `${port}`), no usar `??`. Evitar `&&` para dependencias (usar `cmd1; if ($?) { cmd2 }`).
- **Ejecución larga:** Usar `Start-Process` detached + polling (evita timeout del wrapper).

## Próximos pasos sugeridos

1. **Rotar `AI_API_KEY`** (Groq) — acción manual del usuario (clave expuesta).
2. **Levantar runtime si se desea:** iniciar PostgreSQL (ya está), backend `mvn spring-boot:run` (puerto 8080), frontend `npm run dev` (5173). Actualmente **ambos APAGADOS**.
3. **Verificación runtime:** `http://localhost:8080/actuator/health` UP, proxy `/api` → 8080 OK, login admin OK.
4. **Commit opcional:** cambios móviles (no solicitados para commitear; se mantienen locales).
5. **Backend `mvn test`:** 218/218 OK (sin cambios).
6. **Frontend tests/lint/build:** OK tras ajustes móviles.
