# Auditoría — AI Shopping Assistant

**Fecha:** 2026-10-12
**Alcance:** Análisis previo (FASE 0) para integrar un asistente de compras con IA en `joyeria-main-mobile`.
**Estado:** Sin cambios de código. Documento de diagnóstico e integración.

---

## 1. Arquitectura actual

### 1.1 Backend (`joyeria-backend`)

- **Framework:** Spring Boot 3.5.16, Java 25, Maven. Paquete base `com.joyeria`.
- **Capas:** `controller` → `service` → `repository` (Spring Data JPA) → PostgreSQL.
- **Entidades relevantes:**
  - `Product` (`model/Product.java`): `id`, `name`, `slug`, `description`, `price` (BigDecimal), `comparePrice`, `sku`, `stock` (Integer, real), `color`, `size`, `features`, `careInstructions`, `deliveryTime`, `featured`, `isNew`, `bestSeller`, `active`, `soldCount`; relaciones `@ManyToOne Category`, `@ManyToMany Set<Material>` (tabla `product_materials`), `@OneToMany ProductImage`, `@OneToMany Inventory`.
  - `Category` (`model/Category.java`): `id`, `name`, `slug`, `description`, `displayOrder`, `active`.
  - `Material` (`model/Material.java`): `id`, `name`, `slug`, `description`, `active`.
  - `Inventory`: trazabilidad de stock (INITIAL/SALE/RESTOCK/...). El stock "vivo" es `Product.stock`.
- **Repositorios:** `ProductRepository` ya expone consultas reutilizables:
  - `searchFiltered(query, categoryId, materialId, minPrice, maxPrice, color, inStock, Pageable)` — filtro combinado con `LIKE` sobre nombre/descripción/sku/color.
  - `findFiltered(...)`, `searchProducts(...)`, `findByActiveTrue(...)`, `findTop10ByActiveTrueOrderBySoldCountDesc()`.
  - `CategoryRepository.findByActiveTrueOrderByDisplayOrderAsc()`, `findBySlug`, `findByName` (vía `existsByName`).
  - `MaterialRepository.findByActiveTrue()`, `findBySlug`.
- **DTOs:** `ProductDTO` (con `materialNames`, `categoryName`, `images`, `discountPercent`, `averageRating`, `stock`), `ProductImageDTO`, `PagedResponse<T>`, `ApiResponse<T>` (`{success,message,data}`).
- **Servicios:** `ProductService` (listado, filtros, búsqueda, CRUD admin, mapeo a DTO y cálculo de ratings). `MaterialService`, `CategoryService`.
- **Errores:** `GlobalExceptionHandler` (`@RestControllerAdvice`) centraliza 400/401/403/404/500 y devuelve `ApiResponse`.
- **Seguridad:** `SecurityConfig` stateless con JWT. Públicos: `GET /api/products*`, `/api/categories/**`, `/api/materials/**`, `/api/search/**`, `/api/reviews/**`, `POST /api/newsletter/**`, `/api/payments/webhook/**`, `/uploads/**`, `/actuator/health`. El resto requiere autenticación; `/api/admin/**` requiere rol ADMIN.
- **Rate limiting:** `RateLimitFilter` global (por IP+ruta): 120 req/min por defecto; reglas específicas para login/register/orders/cupones. **Cubre automáticamente `/api/ai/**`** (regla por defecto), y permite añadir una regla específica.
- **Cliente HTTP:** Spring 6 `RestClient` ya se usa en `integration/payment/MercadoPagoClient.java` (patrón aislado y sustituible en tests). **No hay** cliente de IA ni `@ConfigurationProperties` todavía (se usa `@Value`).
- **Integración opcional ya existente (patrón a imitar):** `PaymentProvider` (interfaz) + `SandboxPaymentProvider` + `MercadoPagoPaymentProvider`, seleccionados por `payment.provider` (`PAYMENT_PROVIDER`).
- **Configuración:** todo por variables de entorno en `application.yml` / `application-prod.yml`. No hay variables de IA.

### 1.2 Frontend (`joyeria-frontend`)

- React 18 + TypeScript + Vite + Tailwind + `framer-motion` + `lucide-react` + `axios` + React Router 6.
- **HTTP:** `services/api.ts` (instancia axios, `baseURL=/api` o `VITE_API_URL`, cabecera JWT, refresh automático ante 401). Los servicios (`product.service.ts`, etc.) devuelven `data.data`.
- **Producto UI:** `components/ProductCard.tsx` (usa `Price`, `Rating`, `SmartImage`, `framer-motion`), `components/Price.tsx`, `components/SmartImage.tsx` (fallback de imagen), `utils/format.ts` (`formatPrice` en COP).
- **Estado global:** `AuthContext`, `CartContext` (`addItem(productId, quantity)`), `ToastContext` (`toast(message, type)`). Providers en `main.tsx`.
- **Layout/Rutas:** `App.tsx` define las rutas del storefront dentro de `layouts/MainLayout.tsx`. `CookieConsent` es un ejemplo de overlay global montado fuera de rutas.
- **No** existe capa de IA, ni carpeta `docs/`.

---

## 2. Puntos de integración

```
Frontend (AiAssistantWidget)
   ↓  POST /api/ai/assistant  { message }
API Gateway / Seguridad (JWT + RateLimitFilter)
   ↓
AiAssistantController
   ↓
AiAssistantService  ──>  AiPromptService (prompt + parseo de intención)
   │                  └→  AiChatClient (proveedor LLM, opcional / fallback)
   ↓
ProductSearchService  →  ProductRepository (SQL/JPQL real)
↓
ProductRecommendationService (scoring sobre productos REALES)
↓
PostgreSQL  (única fuente de verdad: precios, stock, materiales, categorías)
```

**Puntos concretos de integración:**

| Punto | Ubicación | Uso |
|---|---|---|
| Nuevo endpoint | `POST /api/ai/assistant` | Recibe `{message}`, devuelve mensaje + productos reales. |
| Búsqueda de productos reales | `ProductRepository.searchFiltered` / `findByActiveTrue` | Fuente única de productos. |
| Categorías / materiales reales | `CategoryRepository`, `MaterialRepository` | Valida nombres que proponga la IA. |
| DTO de salida | `ProductDTO` reutilizado | Precio, stock, materiales, imagen reales. |
| Envoltorio de respuesta | `ApiResponse<T>` | Consistencia con el resto de la API. |
| Configuración IA | `application.yml` + `.env.example` | `AI_ENABLED`, `AI_PROVIDER`, `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL`, `AI_TIMEOUT_SECONDS`. |
| Seguridad | `SecurityConfig` | Regla para `/api/ai/**`. |
| Rate limiting | `RateLimitFilter` | Regla específica `POST|/api/ai/assistant`. |
| Cliente HTTP | Patrón `MercadoPagoClient` (`RestClient`) | Sustituible en tests. |
| UI | `MainLayout` + nuevo `AiAssistantWidget` | Botón flotante + chat. |
| Carrito | `CartContext.addItem` | "Agregar al carrito" desde el chat. |

---

## 2b. Decisión de integración (contrato)

- **Endpoint:** `POST /api/ai/assistant` (público, sin JWT). Decisión: el asistente está disponible para **invitados y usuarios** en la tienda; **no lee ni devuelve datos personales** del usuario. Se protege con validación de entrada y rate limiting. (Alternativa considerada: exigir sesión → documentada en `AI_SHOPPING_ASSISTANT.md`).
- **Request:** `{"message": "Busco un regalo elegante para mi novia de máximo 400000"}`.
- **Response:** `{"message": "...", "products": [{"id", "name", "slug", "price", "image", "material", "available", "reason"}]}`.
- **Regla de oro:** el backend consulta productos reales primero; la IA solo interpreta intención y redacta texto. Nunca se aceptan precios/stock del frontend ni se permite que la IA los genere.

---

## 3. Archivos que serán modificados / creados

### Backend — nuevos (`joyeria-backend/src/main/java/com/joyeria/ai/`)
- `AiAssistantController.java` — endpoint `POST /api/ai/assistant`.
- `AiAssistantService.java` — orquestación + fallback.
- `ProductSearchService.java` — consulta de productos reales según intención.
- `ProductRecommendationService.java` — scoring y motivo por producto.
- `AiPromptService.java` — system/user prompt + parseo determinista de intención.
- `SearchIntent.java` — modelo de intención (categoría, material, precios, color, disponibilidad, estilo, ocasión, regalo, keywords).
- `SearchIntentParser.java` — extracción determinista (funciona sin IA).
- `client/AiChatClient.java` (interfaz) + `client/OpenAiCompatibleChatClient.java` + `client/DisabledAiChatClient.java`.
- `config/AiProperties.java` — `@ConfigurationProperties(prefix="ai")`.
- `dto/AiAssistantRequest.java`, `dto/AiAssistantResponse.java`, `dto/AiProductSuggestion.java`.

### Backend — modificados
- `application.yml` — bloque `ai:` con defaults seguros.
- `application-prod.yml` — (opcional) ajustes de logging.
- `SecurityConfig.java` — regla explícita para `/api/ai/**`.
- `RateLimitFilter.java` — regla específica para `POST|/api/ai/assistant`.
- `GlobalExceptionHandler.java` — (si hace falta) manejo de errores del asistente.
- `pom.xml` — **sin dependencias nuevas** (se reutiliza `RestClient` de Spring 6).

### Backend — tests (`src/test/java/com/joyeria/ai/`)
- `SearchIntentParserTest`, `ProductRecommendationServiceTest`, `AiAssistantServiceTest`, `AiAssistantControllerTest`.

### Frontend — nuevos
- `src/services/ai.service.ts`
- `src/components/AiAssistantWidget.tsx` (botón flotante + ventana de chat + tarjetas de producto)
- `src/services/ai.service.test.ts`, `src/components/AiAssistantWidget.test.tsx`

### Frontend — modificados
- `src/types/index.ts` — tipos `AiAssistantResponse`, `AiProductSuggestion`.
- `src/layouts/MainLayout.tsx` — monta el widget en la tienda.
- `.env.example` — variables de IA (backend) y `VITE_AI_ASSISTANT_ENABLED` (frontend).

### Documentación
- `docs/AI_SHOPPING_ASSISTANT_AUDIT.md` (este documento).
- `docs/AI_SHOPPING_ASSISTANT.md` (entrega final).

---

## 4. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
|---|---|---|
| La IA inventa productos/precios/stock | Crítico | La IA **nunca** produce el JSON comercial: el backend consulta PostgreSQL y arma la respuesta. La IA solo aporta intención (validada) y redacción. |
| Fuga de datos sensibles del usuario a la IA | Alto | El prompt solo envía el texto del usuario + catálogo reducido (id/nombre/precio/material/categoría). Nunca JWT, email, contraseñas ni datos personales. |
| Coste/abuso del endpoint público | Medio | Rate limiting existente + regla específica; límite de longitud de mensaje; límite de productos enviados a la IA. |
| Indisponibilidad/latencia de la IA (timeout) | Alto | `fallback` determinista: la búsqueda y el scoring funcionan sin IA. Errores de IA → respuesta genérica + productos reales. |
| Mensajes maliciosos (prompt injection) | Medio | Se instruye a la IA a responder solo con JSON de intención; se valida contra catálogos reales; se limita tamaño del mensaje. |
| Alucinación de categorías/materiales | Medio | Los nombres propuestos por la IA se validan contra `CategoryRepository`/`MaterialRepository`; si no existen, se descartan. |
| Regresión en listados/búsqueda existentes | Medio | Se reutilizan consultas y DTO existentes; se añaden tests; no se modifican contratos de endpoints actuales. |
| Precio/stock desactualizado en la ventana de chat | Bajo | El precio mostrado es el vigente en el momento de la consulta; el pedido se rige por el stock/precio del backend al agregar al carrito. |
| IA no configurada (sin API key) | Bajo | `AI_ENABLED=false` por defecto: el asistente funciona en modo determinista sin llamar a ningún proveedor. |

---

## 5. Dependencias necesarias

- **Backend:** ninguna nueva. `RestClient` (Spring 6.2, incluido en `spring-boot-starter-web`) + Jackson (ya presentes). Configuración por `@ConfigurationProperties`.
- **Frontend:** ninguna nueva. `axios`, `framer-motion`, `lucide-react` ya presentes.
- **Infraestructura:** variables de entorno opcionales para un proveedor compatible con OpenAI (u otro). Sin proveedor configurado, no hay dependencia externa.

---

## 6. Conclusión

La integración es viable sin cambios arquitectónicos disruptivos. La clave es separar **interpretación de intención** (IA, opcional) de **datos comerciales** (PostgreSQL, obligatorio). El asistente funcionará siempre, con o sin IA, mediante un parser determinista y un ranking por relevancia; y jamás inventará productos, precios ni stock.
