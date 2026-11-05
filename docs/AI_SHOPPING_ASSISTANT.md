# Asistente de compras con IA (AURA)

Implementación de un asistente de compras en lenguaje natural para la tienda.
La IA **solo interpreta la intención** y **redacta** la respuesta; todos los datos
comerciales (productos, precios, stock, disponibilidad, enlaces) provienen
siempre de PostgreSQL. La IA nunca inventa productos ni precios.

- Auditoría previa: `docs/AI_SHOPPING_ASSISTANT_AUDIT.md`.
- Endpoint: `POST /api/ai/assistant` (público, con rate limiting).
- Modo por defecto: **determinista** (`AI_ENABLED=false`), sin proveedor externo.

---

## 1. Arquitectura

```
Frontend (widget) ──POST /api/ai/assistant──▶ AiAssistantController
                                                   │
                                                   ▼
                                            AiAssistantService
                     ┌─────────────────────────────┼─────────────────────────────┐
                     ▼                             ▼                             ▼
            SearchIntentParser            ProductSearchService          AiPromptService
        (intención determinista:      (consulta real a PostgreSQL    (fase IA opcional 1:
         precio, material, categoría,   vía ProductRepository)         interpretar y fase 2:
         color, estilo, ocasión,                                        redactar mensaje)
         género, regalo, keywords)                │                             │
                     └──────────────┬───────────────┘                       AiChatClient
                                    ▼                                     (OpenAiCompatible /
                          ProductRecommendationService                     Disabled)
                          (scoring sobre datos reales)
                                    │
                                    ▼
                          AiAssistantResponse { message, products[] }
```

La IA es **opcional y de dos fases**; ambas degradan a lógica determinista si el
proveedor falla o no está configurado.

### Paquete backend `com.joyeria.ai`

| Clase | Responsabilidad |
| --- | --- |
| `AiAssistantController` | `POST /api/ai/assistant`, valida la entrada. |
| `AiAssistantService` | Orquesta intención → búsqueda → recomendación → redacción, con fallback. |
| `SearchIntentParser` | Extrae la intención de forma determinista (regex + stopwords). |
| `SearchIntent` | Modelo inmutable de la intención. |
| `TextNormalizer` | Normaliza texto (minúsculas, acentos, espacios). |
| `ProductSearchService` | Consulta real al catálogo (`ProductRepository.searchFiltered`). |
| `ProductRecommendationService` | Puntúa y ordena resultados reales. |
| `AiPromptService` | Construye prompts y valida que la IA solo use datos reales. |
| `AiUnavailableException` | Señala indisponibilidad del proveedor para activar el fallback. |
| `config/AiProperties` | Propiedades `ai.*`. |
| `client/AiChatClient` (+ `OpenAiCompatibleChatClient`, `DisabledAiChatClient`, `AiClientConfig`) | Abstracción del proveedor (compatible OpenAI vía `RestClient`). |
| `dto/AiAssistantRequest`, `dto/AiAssistantResponse`, `dto/AiProductSuggestion` | Contratos de entrada/salida. |

---

## 2. Contrato de la API

`POST /api/ai/assistant` — `Content-Type: application/json`

Petición:

```json
{ "message": "busco un anillo de plata por menos de 200.000 para regalo" }
```

- `message`: obligatorio, no vacío, máximo 500 caracteres (`AI_MAX_MESSAGE_LENGTH`).

Respuesta (`ApiResponse<AiAssistantResponse>`):

```json
{
  "success": true,
  "data": {
    "message": "Encontré algunas opciones que podrían interesarte.",
    "products": [
      {
        "id": 12,
        "name": "Anillo Aurora",
        "slug": "anillo-aurora",
        "price": 185000,
        "image": "/uploads/anillo-aurora.webp",
        "material": "Plata 925",
        "available": true,
        "reason": "Cumple tu rango de precio y es ideal para regalar."
      }
    ]
  },
  "timestamp": "2026-01-01T12:00:00Z"
}
```

`products` solo contiene piezas reales del catálogo (máximo `AI_MAX_PRODUCTS`,
por defecto 4). Si no hay coincidencias, `products` va vacío y el mensaje lo
explica. Errores de validación → `400` (manejados por el `GlobalExceptionHandler`).

---

## 3. Variables de entorno

Backend (`application.yml`, bloque `ai:`):

| Variable | Por defecto | Descripción |
| --- | --- | --- |
| `AI_ENABLED` | `false` | `true` activa el proveedor externo; `false` = modo determinista. |
| `AI_PROVIDER` | `openai-compatible` | Tipo de proveedor. |
| `AI_API_KEY` | _(vacío)_ | Clave del proveedor (solo backend; **nunca** en el frontend). |
| `AI_BASE_URL` | `https://api.openai.com/v1` | Base URL compatible con OpenAI. |
| `AI_MODEL` | `gpt-4o-mini` | Modelo a usar. |
| `AI_TIMEOUT_SECONDS` | `15` | Timeout de las llamadas a la IA. |
| `AI_MAX_MESSAGE_LENGTH` | `500` | Longitud máxima del mensaje. |
| `AI_MAX_PRODUCTS` | `4` | Número máximo de productos sugeridos. |

Frontend:

| Variable | Por defecto | Descripción |
| --- | --- | --- |
| `VITE_AI_ASSISTANT_ENABLED` | `true` | `false` oculta el widget por completo. |

---

## 4. Seguridad

- **Público**: `POST /api/ai/assistant` está en `permitAll` (`SecurityConfig`),
  disponible para invitados y usuarios. No solicita ni expone datos personales.
- **Rate limiting**: regla `POST|/api/ai/assistant` → 20 peticiones / 60 s por IP
  (`RateLimitFilter`).
- **Validación**: `@Valid` + `@NotBlank` + `@Size(max = 500)` en la petición.
- **Sin alucinaciones**: los productos/precios se leen de la base de datos; la IA
  solo redacta. Cualquier dato devuelto es el de catálogo.
- **Sin claves en el cliente**: la `AI_API_KEY` vive solo en el backend.
- **Degradación segura**: si el proveedor tarda o falla, se responde con la
  interpretación determinista y el mensaje por defecto.

---

## 5. Frontend

| Archivo | Descripción |
| --- | --- |
| `src/services/ai.service.ts` | `aiService.ask(message)` → `POST /ai/assistant`; exporta `aiAssistantEnabled`. |
| `src/components/AiAssistantWidget.tsx` | Botón flotante + panel de chat (mensajes, sugerencias, "Agregar", "Ver"). |
| `src/layouts/MainLayout.tsx` | Monta el widget en el storefront. |
| `src/types/index.ts` | Tipos `AiAssistantResponse`, `AiProductSuggestion`. |

Comportamiento del widget:

- Botón flotante (abajo a la derecha); se oculta si `VITE_AI_ASSISTANT_ENABLED=false`.
- Saludo inicial, entrada (máx. 500), indicador "Buscando productos…".
- Tarjetas de producto con imagen (`SmartImage`), nombre, precio (`formatPrice`),
  motivo opcional, enlace "Ver" (a `/productos/:slug`) y "Agregar".
- "Agregar" requiere sesión: si no hay usuario, avisa y lleva a `/login`.
- Errores de red/proveedor muestran un mensaje de respaldo; el usuario nunca ve
  un fallo crudo.
- Responsive (ancho del panel acotado con `min()`/`max-h`).

---

## 6. Pruebas

Backend (`joyeria-backend/src/test/java/com/joyeria/ai/`):

- `SearchIntentParserTest` — extracción de intención (precio, material, categoría, etc.).
- `ProductRecommendationServiceTest` — scoring y orden.
- `AiAssistantServiceTest` — orquestación y fallback determinista.
- `AiAssistantControllerTest` — contrato HTTP (200/400/500) con MockMvc.

Frontend:

- `src/services/ai.service.test.ts` — llamada al endpoint.
- `src/components/AiAssistantWidget.test.tsx` — abrir/consultar/render de productos,
  agregar al carrito, invitado → login, fallback de error.

Verificación ejecutada:

- `mvn -o test` → **144/144** ✅ (25 nuevos del asistente, sin regresiones).
- `npm run lint` → 0 errores (solo warnings preexistentes).
- `npm test` → **26/26** ✅.
- `npm run build` → build de producción correcto.

---

## 7. Archivos nuevos / modificados

**Backend — nuevos**: todo `com.joyeria.ai/**` (controlador, servicio, parser,
normalizador, búsqueda, recomendación, prompts, propiedades, excepción, `client/**`
y `dto/**`).

**Backend — modificados**: `service/ProductService.java` (`mapProducts`),
`config/SecurityConfig.java`, `security/RateLimitFilter.java`,
`resources/application.yml`.

**Frontend — nuevos**: `services/ai.service.ts`,
`components/AiAssistantWidget.tsx` y sus tests.

**Frontend — modificados**: `layouts/MainLayout.tsx`, `types/index.ts`,
`vite-env.d.ts`.

**Raíz**: `.env.example` (variables `AI_*` + `VITE_AI_ASSISTANT_ENABLED`),
`docs/AI_SHOPPING_ASSISTANT.md` y `docs/AI_SHOPPING_ASSISTANT_AUDIT.md`.

---

## 8. Límites y recomendaciones futuras

- **Sin base vectorial**: la búsqueda es por filtros deterministas + scoring;
  suficiente para el catálogo actual. Para catálogos grandes, evaluar embeddings.
- **Proveedor único**: soporta cualquier endpoint compatible con OpenAI
  (`AI_BASE_URL`). Añadir otros proveedores = nueva implementación de `AiChatClient`.
- **Coste/abuso**: el rate limiting es por IP; si el tráfico crece, considerar
  límites por cuenta y caché de respuestas frecuentes.
- **Observabilidad**: registrar métricas de uso y de fallback de IA (sin datos
  personales) para afinar prompts y modelos.
