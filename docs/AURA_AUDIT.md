# AURA AI — Auditoría y diagnóstico (Fase 1)

> Documento de **solo diagnóstico**. No se ha modificado código para producir esta
> auditoría. Base para las Fases 2–8 descritas en `§7`.
>
> Fecha: 2026-10-12 · Estado de la app: backend y frontend vivos en localhost
> (8080 / 5173). Rama `main` con 6 commits locales sin subir; cambios de la
> mejora de búsqueda **sin commitear** (ver `§4.7`).

---

## 1. Arquitectura actual de AURA

AURA **no es un chatbot genérico**: es un pipeline **híbrido determinista + IA**.
El parser determinista siempre actúa; la IA es **opcional y amplía** el parser.

```
AiAssistantWidget.tsx (React)
  │  POST /api/ai/assistant  { message }
  ▼
AiAssistantController                      (valida @Valid, público)
  ▼
AiAssistantService.assist(message)         (orquestador, @Transactional readOnly)
  ├─ SearchIntentParser.parse(message)     (siempre: extrae intención)
  ├─ ProductSearchService.findCandidates   (consulta PostgreSQL: única fuente de datos)
  ├─ [si IA habilitada] AiChatClient.interpretIntent → SearchIntentParser.merge
  ├─ [si IA habilitada] repetir findCandidates con intención refinada
  ├─ ProductRecommendationService.recommend (puntúa y ordena)
  └─ resolveMessage: AiChatClient.composeMessage (IA) o defaultMessage (fijo)
  ▼
AiAssistantResponse { message, products: [AiProductSuggestion] }
```

| Pieza | Archivo | Rol |
|---|---|---|
| Widget visual | `joyeria-frontend/src/components/AiAssistantWidget.tsx` | Chat flotante, envía mensajes, dibuja tarjetas. |
| Servicio front | `joyeria-frontend/src/services/ai.service.ts` | `aiService.ask(message)` → `POST /ai/assistant`. |
| Tipos front | `joyeria-frontend/src/types/index.ts` (`AiAssistantResponse`, `AiProductSuggestion`) | Contrato actual (solo `message` + `products`). |
| Endpoint | `joyeria-backend/.../ai/AiAssistantController.java:30` | `POST /api/ai/assistant`. |
| Orquestación | `.../ai/AiAssistantService.java` | Valida, interpreta, busca, recomienda, redacta. |
| Parser determinista | `.../ai/SearchIntentParser.java` | Extrae categoría/material/color/estilo/ocasión/género/precio/keywords. |
| Búsqueda catálogo | `.../ai/ProductSearchService.java` | `ProductRepository.searchFiltered` (no SQL crudo de IA). |
| Recomendación | `.../ai/ProductRecommendationService.java` | Puntuación por relevancia. |
| Prompt | `.../ai/AiPromptService.java` | System prompts + parseo del JSON de intención. |
| Proveedor IA | `.../ai/client/*` (`OpenAiCompatibleChatClient`, `DisabledAiChatClient`, `AiClientConfig`) | Compatible OpenAI. Si no hay key → cliente inactivo. |
| Config IA | `.../ai/config/AiProperties.java` + `application.yml:82-92` + `.env.example:51-63` | Variables `AI_*`. |

**Proveedor/modelo:** configurables (`AI_PROVIDER=openai-compatible`,
`AI_MODEL=gpt-4o-mini`, `AI_BASE_URL=https://api.openai.com/v1`). Por defecto
**`AI_ENABLED=false`** → `DisabledAiChatClient` (`AiClientConfig.java:20-27`), es
decir, el modo por defecto de AURA es **100 % determinista, sin proveedor**.

---

## 2. Respuestas a los 11 puntos de la auditoría

1. **Componente visual** → `AiAssistantWidget.tsx`.
2. **Servicio que procesa mensajes** → backend `AiAssistantService.assist` (front: `ai.service.ts`).
3. **Endpoint** → `POST /api/ai/assistant`.
4. **Proveedor y modelo IA** → compatible OpenAI, `AI_MODEL` (def. `gpt-4o-mini`); **deshabilitado por defecto**.
5. **Prompt del sistema actual** → `AiPromptService.intentSystemPrompt` (analizador JSON), `composeSystemPrompt` (redacción, "máximo 2 frases"), `composeUserPrompt`.
6. **Consultas al catálogo** → `ProductSearchService` → `ProductRepository.searchFiltered`.
7. **Campos** → `AiProductSuggestion`: `id, name, slug, price, image, material, available, reason`. `ProductDTO` aporta categoría, materiales, color, tamaño, stock, soldCount.
8. **Historial de conversación** → **no existe en el backend**; el front lo guarda solo para pintarlo (`AiAssistantWidget.tsx:30,54,60`) y **nunca lo reenvía**.
9. **Tarjetas de recomendación** → renderizadas en `AiAssistantWidget.tsx:130-166` a partir de `response.products`.
10. **Productos de prueba** → 2 filas reales en PostgreSQL: `Smoke Prod 639273693565996804` (id 15) y `Smoke Prod 639273781962910746` (id 16).
11. **Auth/validaciones/errores/env** → endpoint **público** (`SecurityConfig.java:78`), `@Valid` (`AiAssistantRequest`), rate limit `POST|/api/ai/assistant → 20/60s` (`RateLimitFilter.java:34`), variables `AI_*` / `VITE_AI_ASSISTANT_ENABLED`.

---

## 3. Causa raíz por síntoma

| # | Síntoma reportado | Causa raíz (real) | Evidencia |
|---|---|---|---|
| 1 | AURA muestra productos ante un saludo | **No hay clasificación de intención.** `assist()` ejecuta el pipeline completo siempre: parsea y busca sin importar el mensaje. Un saludo produce una `SearchIntent` vacía → `findCandidates` sin filtros → devuelve el top-50 por `soldCount` → recomienda 4. | `AiAssistantService.java:54-55`; `ProductSearchService.java:44-58,99-110` |
| 2 | Respuestas genéricas y repetidas | Con IA deshabilitada (def.), el texto es **fijo**: `defaultMessage()`. Aun con IA, `composeSystemPrompt` es genérico y no conoce la intención real ni el historial. | `AiAssistantService.java:113-118`; `AiPromptService.java:43-53` |
| 3 | Nombres técnicos "Smoke Prod …" | Son **productos de prueba reales** creados por el script de smoke `POST /api/admin/products` `name="Smoke Prod $stamp"` (precio 100000, stock 4/3, categoría *Anillos*, 1 imagen, **sin materiales**). El asistente los expone porque no filtra registros incompletos. No es un bug de render. | DB: ids 15–16; script `%TEMP%\opencode\smoke6.ps1` |
| 4 | Productos que no cumplen presupuesto/intención | (a) Intención vacía ⇒ sin presupuesto ⇒ se muestran todos. (b) El *fallback* `ANY_PRICE` convierte **cualquier número suelto** en `maxPrice`, generando falsos presupuestos. (c) La recomendación solo **suma** puntos por presupuesto; no lo impone como regla dura (aunque hoy la consulta sí filtra). | `SearchIntentParser.java:153-158` (fallback); `ProductRecommendationService.java:69-73` (score, no filtro) |
| 5 | No se conserva el contexto | El contrato de entrada es **solo `message`**; no hay sesión ni historial. "Para mi novia" tras "busco un regalo" pierde "regalo". `merge()` solo combina parser+IA de **un** mensaje. | `AiAssistantRequest.java:18`; `SearchIntentParser.java:114-136` |
| 6 | Conversación/tarjetas/recomendaciones poco integradas | El contrato de respuesta es solo `{message, products}`: sin `intent`, sin `followUpQuestion`, sin `suggestions`. El front no distingue búsqueda de charla ni muestra preguntas de seguimiento. | `AiAssistantResponse.java:17-19` (`§10` del prompt) |
| 7 | Experiencia visual mejorable | Widget funcional pero plano: indicador "Buscando productos…" fijo, sin *typing* real, sin chips de sugerencias, nombre truncado, sin estado "agotado" visible, sin manejo de foco/Enter-multilínea. | `AiAssistantWidget.tsx:20-24,138,171-177` |

---

## 4. Hallazgos adicionales

### 4.1 El parser determinista ya cubre mucho
`SearchIntentParser` reconoce material, categoría, color, estilo, ocasión, género,
precio (`entre/menos de/más de`) y `keywords`, con `STRUCTURED_TERMS` para no
duplicar puntuación. **Es la base sólida** sobre la que construir la clasificación
de intención, no un problema.

### 4.2 Presupuesto: filtrado real vs. puntuación
La consulta **sí** aplica `minPrice`/`maxPrice` (`ProductSearchService.java:106-107`)
y la relajación progresiva (`:49-57`) conserva los precios. El riesgo real es
**cuándo no se detecta** el presupuesto (fallback `ANY_PRICE`) o **cuando no hay
intención**. Recomendación: hacer del presupuesto explícito un **filtro duro** y
eliminar/limitar el fallback de número suelto.

### 4.3 `merge()` es anti-alucinación correcto
`validateAgainst()` descarta categoría/material de la IA que no existan en el
catálogo real (`SearchIntentParser.java:246-261`). Mantenerlo.

### 4.4 Duplicación de límite de mensaje
`AiAssistantRequest` fija `@Size(max = 500)` **hardcodeado** (`:17`) además de
`AiProperties.maxMessageLength` (`:36`) y la comprobación en `assist()`
(`:49-52`). Si se cambia `AI_MAX_MESSAGE_LENGTH`, el `@Valid` sigue rechazando a 500.

### 4.5 Modo por defecto = determinista
Sin `AI_ENABLED=true` + `AI_API_KEY`, **no hay redacción por IA**: el texto es
plantilla. Cualquier evaluación de "naturalidad" debe considerar ambos modos.

### 4.6 Seguridad (estado actual, correcto)
Endpoint público para invitados, sin datos personales, con rate limit por IP y
validación de entrada. La clave de IA vive solo en el backend. `RateLimitFilter`
aplica `X-RateLimit-Remaining`. No hay persistencia de conversaciones (bien para
privacidad; a revisar al añadir contexto en Fase 4).

### 4.7 Estado de Git y copias del backend (riesgo operativo)
- El servidor vivo (PID 6276/19098) corre **desde el repo**
  `joyeria-main-mobile\joyeria-backend` vía `mvn spring-boot:run`.
- **Existe una copia obsoleta** en `Default Project\joyeria-backend` **sin el
  paquete `ai`** (jar del 2026-09-17). Los scripts `encender.ps1` y
  `prender-pagina.ps1` apuntan a esa copia → **arrancarían un backend antiguo sin
  AURA**. Editar/probar/compilar siempre en el repo.
- Cambios actuales **sin commitear** en el repo: `AiAssistantService`,
  `ProductRecommendationService`, `ProductSearchService`, `SearchIntentParser`,
  `GiftFinderService`, `ProductRepository`, 2 tests editados y
  `ProductSearchServiceTest` nuevo (mejora de búsqueda + fix `materialId`).

---

## 5. Archivos afectados (por fase)

- **Backend AURA:** `AiAssistantService`, `AiAssistantController`, `SearchIntentParser`, `SearchIntent`, `ProductSearchService`, `ProductRecommendationService`, `AiPromptService`, `TextNormalizer`, `dto/AiAssistantRequest`, `dto/AiAssistantResponse`, `dto/AiProductSuggestion`, `client/*`, `config/AiProperties`.
- **Backend catálogo:** `repository/ProductRepository` (`searchFiltered`), `service/ProductService` (`mapProducts`).
- **Frontend:** `components/AiAssistantWidget.tsx`, `services/ai.service.ts`, `types/index.ts`, `utils/{format,errors}.ts`, `contexts/CartContext`.
- **Config/seguridad:** `config/SecurityConfig.java`, `security/RateLimitFilter.java`, `exception/GlobalExceptionHandler.java`, `application.yml`, `.env.example`.
- **Datos:** filas `products` id 15–16 ("Smoke Prod") en la BD de desarrollo.
- **Tests:** `ai/*Test` (backend) y `AiAssistantWidget.test.tsx`, `ai.service.test.ts` (frontend).
- **Docs:** este `docs/AURA_AUDIT.md`; pendiente `docs/AURA_IMPLEMENTATION.md`.

---

## 6. Restricciones confirmadas (no violar)

- La IA **nunca** inventa ni define datos comerciales; PostgreSQL es la única fuente.
- La IA **no** consulta la BD directamente: siempre vía `ProductSearchService`.
- No reescribir el proyecto, no cambiar de stack, no romper catálogo/auth/carrito.
- API keys solo en backend. No confiar en precios/ids del frontend.
- Usar frameworks de test ya instalados (JUnit/MockMvc; Vitest/RTL).

---

## 7. Plan de corrección por fases (mapea al `§15` del prompt)

### Fase 2 — Clasificación de intención
Nuevo `AiIntent` (enum) + `IntentClassifier` determinista: `GREETING`,
`SMALL_TALK`, `PRODUCT_SEARCH`, `PRODUCT_DETAILS`, `BUDGET_SEARCH`,
`GIFT_ADVICE`, `CATALOG_QUESTION`, `CLARIFICATION`, `OUT_OF_SCOPE`.
`assist()` decide **antes** de buscar: charla/small talk → **sin consulta ni
tarjetas**. Tolerante a tildes (usar `TextNormalizer`).
→ Tests obligatorios 1, 2, 3.

### Fase 3 — Filtros y recomendaciones con datos reales
- Presupuesto explícito como **filtro duro**; acotar/eliminar fallback `ANY_PRICE`.
- Recomendación: filtrar agotados cuando se pide disponibilidad antes de puntuar.
- Añadir `intent` a la respuesta para trazabilidad.
→ Tests 4, 5, 8, 9, 10.

### Fase 4 — Contexto conversacional
`ConversationContext` (dict: `intent, productType, material, minPrice, maxPrice,
occasion, style, availability, lastRecommendedProductIds`) + `context` opcional en
el request; merge no destructivo (complementa, no borra; actualiza si el usuario
cambia presupuesto/tema). Sin persistencia en servidor; contexto por sesión del
cliente. Limitar tamaño. Aislado por sesión (sin compartir entre usuarios).
→ Tests 6, 7, 14.

### Fase 5 — Prompt del sistema y respuestas estructuradas
Reescribir `AiPromptService` con el prompt AURA del `§9`. Extender
`AiAssistantResponse` con `intent`, `followUpQuestion`, `suggestions` (campos
nuevos → compat: el front los ignora si faltan). Validar la salida del proveedor
con *fallback* seguro.
→ Tests 2, 12, 13.

### Fase 6 — Corrección de datos y tarjetas
Detectar/ocultar registros técnicos/incompletos (nombre tipo "Smoke Prod" +
sin material/descripción), registrar el problema para admin (sin borrar datos).
`Smoke Prod` proviene del seed de smoke; documentar y excluir de recomendaciones.
→ Test 11, 15.

### Fase 7 — Interfaz y estados de carga
`typing`, estado de búsqueda real, chips de sugerencias que **envían mensajes
reales**, Enter envía / multilínea, anti-doble-envío, estado "agotado", scroll
inteligente, responsive, errores comprensibles.
→ Tests 15, 16.

### Fase 8 — Pruebas, seguridad y documentación
Completar los 16 escenarios, revisar rate limit/validaciones, y escribir
`docs/AURA_IMPLEMENTATION.md`.

### Criterios de aceptación
Ver `§16` del prompt maestro. Al cierre: AURA distingue saludo de búsqueda,
mantiene contexto, respeta filtros, no inventa datos ni expone nombres técnicos,
UI responsive con errores recuperables, catálogo/auth/carrito intactos, tests en
verde y build de front/back correctos.

---

## 8. Limitaciones y riesgos conocidos

- **Sin proveedor IA configurado**, la "naturalidad" depende del texto fijo; las
  mejoras deben funcionar y probarse en modo determinista.
- **Dos copias del backend**: alto riesgo de editar/compilar la equivocada (usar
  siempre `joyeria-main-mobile\joyeria-backend`).
- **Push bloqueado** (403) y cambios sin commitear: consolidar antes de fases largas.
- Datos de prueba en la BD de desarrollo contaminan las recomendaciones.
