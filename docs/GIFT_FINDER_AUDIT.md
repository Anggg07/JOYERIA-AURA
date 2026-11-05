# Auditoría — Smart Gift Finder

FASE 0. Análisis del estado actual antes de implementar. **No se modifica código en
esta fase.** Alcance: encontrar el regalo adecuado mediante un asistente paso a paso
que devuelve productos reales del catálogo.

Stack actual (sin cambios): React 18 + TypeScript + Vite; Spring Boot 3.5 + Spring
Security (JWT) + JPA/Hibernate + PostgreSQL + Flyway; Tailwind; framer-motion.

---

## 1. Modelo de datos (backend)

### `products` (`model/Product.java`, tabla `products`)
Campos: `id, name, slug, description, price (DECIMAL 12,2), comparePrice, sku, stock,
weight, dimensions, size, color, careInstructions, features, deliveryTime, featured,
isNew, bestSeller, active, soldCount, category (ManyToOne), materials (ManyToMany
`product_materials`), images (OneToMany), inventoryRecords, createdAt, updatedAt`.

- **No existen** columnas `occasion`, `style`, `recipient` ni `gender`.
- `price` es el precio real; `comparePrice` el precio tachado; `stock` unidades.
- `getDiscountPercent()` derivado.

### `categories` (`model/Category.java`)
`id, name, slug, description, displayOrder, image, active, createdAt`.
Seed dev: Anillos, Collares, Pulseras, Aretes, Dijes, Sets, Joyería personalizada,
Regalos (`DataInitializer`, perfil `dev`).

### `materials` (`model/Material.java`)
`id, name, slug, description, image, active, createdAt`; lado inverso de `products`.
Seed dev: Oro 18K, Plata 925, Oro blanco, Oro rosa, Piedras preciosas, Piedras
semipreciosas, Acero quirúrgico, Perlas.

### `product_images` (`model/ProductImage.java`)
`id, product, url, alt, isPrimary, sortOrder`. Cada producto seed tiene una imagen.

### `reviews` (`model/Review.java`)
`id, product, user, rating, title, comment, status (PENDING/APPROVED/REJECTED),
createdAt`. Las valoraciones agregadas se calculan por consulta, no hay asociación en
`Product`.

### Migraciones Flyway (`db/migration`)
`V1…V13`. Productos/categorías/materiales en `V1`; índices trigram en `V11`; índices
FK de favoritos en `V13`. **No hay ninguna columna de ocasión/estilo/destinatario.**

---

## 2. Capa de acceso a datos

### `repository/ProductRepository.java`
- `searchFiltered(String query, Long categoryId, Long materialId, BigDecimal minPrice,
  BigDecimal maxPrice, String color, boolean inStock, Pageable)` — JPQL con
  `p.active = true` y filtros opcionales. **Solo** filtra por categoría, material,
  precio, color y stock.
- `findFiltered(...)` (variante sin `query`), derivados (`findByActiveTrue`, etc.).
- **No hay soporte de consulta por estilo/ocasión/destinatario.**

### `CategoryRepository` / `MaterialRepository`
`findBySlug`, `findByActiveTrueOrderByDisplayOrderAsc` / `findByActiveTrue`.

### `ProductService` (`service/ProductService.java`)
- `search(...)`, `getFiltered(...)`, `getAllProducts(...)`, `getProductBySlug(...)`,
  `getRelatedProducts(...)`… devuelven `ProductDTO` o `PagedResponse<ProductDTO>`.
- **`mapProducts(Collection<Product>)`** (público, añadido para el asistente IA):
  mapea en lote con valoraciones; **reutilizable** por el Gift Finder.
- `ProductDTO`: incluye `categoryId, categoryName, materialIds, materialNames, images,
  discountPercent, averageRating, reviewCount`. Ideal para las tarjetas de resultado.

### API pública (`controller/ProductController.java`)
- `GET /api/products` (params `categoryId, materialId, minPrice, maxPrice, color,
  inStock, sort, direction, page, size`), `GET /api/products/{id}`, `/slug/{slug}`,
  `/featured`, `/new`, `/best-sellers`, `/{id}/related`.
- `GET /api/search?q=...` (mismos filtros).
- Todo envuelto en `ApiResponse<T>` (`{success, message, data}`).

### Seguridad (`config/SecurityConfig.java`)
Público (`permitAll`): `/api/products...`, `GET /api/categories`, `GET /api/materials`,
`GET /api/search/**`, `POST /api/newsletter/**`, **`POST /api/ai/assistant`**,
`/uploads/**`, `/actuator/health`. Rate limiting por IP (`security/RateLimitFilter`).

---

## 3. Precedente reutilizable: Asistente de compras IA (`com.joyeria.ai`)

Ya existe un motor que interpreta intención y puntúa productos **reales**:

- `SearchIntent` (`category, material, minPrice, maxPrice, color, style, occasion,
  gender, gift, inStockOnly, keywords`).
- `SearchIntentParser` — extracción determinista (regex + vocabularios) con
  `merge()` anti-alucinación contra el catálogo real.
- `TextNormalizer.normalize()` — normaliza (minúsculas, sin acentos); **reutilizable**.
- `ProductSearchService.findCandidates(intent)` — consulta real + relajación.
- `ProductRecommendationService` — scoring + `haystack()` (name/description/category/
  color/size/features/materials) usado para casar style/occasion/gender al no haber
  columnas.
- `AiAssistantService`, `AiAssistantController` (`POST /api/ai/assistant`),
  `dto/AiProductSuggestion`.

**Conclusión:** el patrón “intención → candidatos reales → scoring en memoria →
tarjetas” ya está probado. El Gift Finder lo replica de forma específica y
determinista, sin cambiar el esquema.

---

## 4. Frontend

### Routing (`src/App.tsx`)
Storefront bajo `<Route path="/" element={<MainLayout />}>` con páginas lazy envueltas
en `<Suspense fallback={<Loader/>}>`. `MainLayout` ya incluye `Navbar`, `Footer` y el
widget de IA. **Punto de inserción:** añadir `GiftFinder` lazy y `path="gift-finder"`.

### Servicios y tipos
- `services/product.service.ts` (`getAll`, `search`, `getBySlug`…),
  `category.service.ts` (`getAll`), `material.service.ts` (`getAll`).
- `types/index.ts`: `Product`, `Category`, `Material`, `ApiResponse<T>`,
  `PagedResponse<T>`, y los tipos IA (`AiProductSuggestion`, `AiAssistantResponse`).

### Componentes reutilizables
`ProductCard` (`product, onAddToCart?, onQuickView?, onToggleFavorite?, isFavorite?`),
`SmartImage` (fallback + lazy), `Price`, `Rating`, `Skeleton`/`ProductCardSkeleton`,
`Button`, `Modal`/`QuickViewModal`, `Pagination`.

### Contextos
`CartContext.addItem(productId, quantity?)` (lanza en error), `ToastContext.toast`,
`AuthContext.isAuthenticated`, `ThemeContext`. Favoritos vía `userService`.

### Estilos y animación
- Tokens HSL en `src/index.css` + `tailwind.config.js`: `primary` (oro), `accent`,
  `background.warm/light`, `surface{,-muted,-elevated}`, `foreground.{muted,faint}`,
  `line`, `ink`; utilidades `.text-primary`, `.text-accent`, `.bg-warm`, `.bg-light`,
  `.font-serif`, `.scrollbar-hide`. Dorado de marca `#C9A227` / `#F5D06F`.
- framer-motion con `useReducedMotion()`; variantes `fadeUp`/`stagger`,
  `AnimatePresence` para pasos. **Modelo directo:** `pages/Personalize.tsx`
  (asistente de 4 pasos con stepper, tarjetas de opción, resumen lateral y `aria-pressed`).

### Estado en URL
`Catalog.tsx` usa `useSearchParams` con `updateParams(key, value)` (patrón a seguir
para URL compartible). `ResetPassword`/`SandboxPaymentPage` lo usan solo lectura.

### Tests
Vitest + Testing Library (`src/test/setup.ts` con mocks de `matchMedia`,
`IntersectionObserver`, `ResizeObserver`). Patrones: `AiAssistantWidget.test.tsx`
(`vi.hoisted` + mocks de contexto/servicio + `MemoryRouter`), `CartContext.test.tsx`
(`renderHook` + `act`/`waitFor`), `auth.service.test.ts` (mock de `./api`). E2E:
Playwright `e2e/purchase-flow.spec.ts`.

---

## 5. Problema central y decisión de diseño

No hay datos estructurados de destinatario, ocasión ni estilo. Opciones evaluadas:

| Opción | Pros | Contras |
| --- | --- | --- |
| A. Añadir columnas `recipient/occasion/style` a `products` | Datos explícitos | Migración + entidad + DTO + admin + seed; **inventar valores** para el catálogo actual; rompe `ddl-auto: validate` si se desalinea; mayor alcance/riesgo |
| B. Tabla `product_tags` normalizada | Flexible | Igual que A en alcance; requiere poblar datos inexistentes |
| C. Categoría/material como señal | Sin cambios de esquema | Ocasión/estilo quedan implícitos |
| **D. Scoring determinista sobre el catálogo real** (reusa patrones de IA) | Sin migración; no inventa datos; reutiliza servicios; alineado con el Asistente IA | Precisión de ocasión/estilo depende del texto real del producto |

**Decisión: Opción D.** El motor puntúa sobre **datos reales** (categoría, materiales,
precio, stock y el texto ya almacenado: nombre, descripción, features, categoría,
color), aplicando afinidades de destinatario/ocasión documentadas y derivadas del
catálogo real. Si falta un dato, el algoritmo **sigue funcionando** (se omiten esos
criterios y la relevancia se recalcula). Nunca se inventan productos ni precios.

Reutilización: `TextNormalizer` (normalización), `ProductService.mapProducts` (mapeo a
DTO), `ProductRepository.searchFiltered` (consulta real), `ApiResponse`, `SecurityConfig`
y `RateLimitFilter`.

---

## 6. Diseño de la solución

### Backend (`com.joyeria.gift`)
- `dto/GiftFinderRequest` — `recipient, occasion, style, material` (String, opcionales,
  normalizados), `budgetMin, budgetMax` (BigDecimal, `@PositiveOrZero`; opcionales).
- `dto/GiftFinderResponse` — `recommendations: List<GiftRecommendation>`.
- `dto/GiftRecommendation` — `id, name, slug, price, image, material (String),
  materialNames (List), categoryName, available (boolean), relevance (Integer, nullable),
  explanation (String)`.
- `GiftFinderService` — orquesta, valida (`min ≤ max`, no negativos → `IllegalArgumentException`
  → 400), consulta real, puntúa, ordena y limita.
- `GiftFinderController` — `POST /api/gift-finder/recommend`, público.
- Integración: `SecurityConfig` (`POST /api/gift-finder/**` permitAll) y
  `RateLimitFilter` (regla `POST|/api/gift-finder/recommend`, 20/60 s).

### Motor de scoring (solo criterios activos)
Pesos: ocasión 20 · material 20 · presupuesto 25 · estilo 15 · destinatario 15 ·
disponibilidad 10. Se puntúa solo lo que el usuario eligió; `relevance = round(100·score/max)`.
Se muestra solo si hay ≥1 criterio del usuario (no se revelan puntuaciones internas).
Orden: score desc → disponible > agotado → `soldCount` desc. Límite 8 resultados.

### Frontend
- `pages/GiftFinder.tsx` — asistente de 5 pasos (destinatario, ocasión, estilo,
  presupuesto, material) con progreso, opciones visuales, “No estoy seguro”, resumen,
  atrás/siguiente y CTA **“ENCONTRAR MI REGALO”**; transiciones framer-motion
  (respetando `useReducedMotion`); diseño premium mobile-first.
- Resultados: “Encontramos estas opciones para ti” con imagen, nombre, precio, material,
  disponibilidad, relevancia (opcional) y explicación corta; añadir al carrito y ver
  producto.
- `services/gift.service.ts` — `recommend(criteria)` → `POST /gift-finder/recommend`.
- URL compartible con `useSearchParams`; validación de parámetros al cargar; botón
  “Compartir” (copia la URL).
- Ruta `gift-finder` en `App.tsx`; enlace en `Navbar` (y/o Home).

### Tests
- Backend: presupuesto, material, ocasión, estilo, sin resultados, agotados, filtros
  incompletos, valores inválidos, combinación sin coincidencias; + contrato del controller.
- Frontend: servicio (`gift.service.test.ts`) y página (`GiftFinder.test.tsx`: pasos,
  envío, resultados, vacío, URL compartible).

### Documentación
`docs/GIFT_FINDER.md` (arquitectura, algoritmo, endpoints, componentes, BD, pruebas).

---

## 7. Riesgos y mitigaciones

- **Precisión de ocasión/estilo por texto** → afinidades documentadas + keywords; si no
  hay coincidencia se explica con presupuesto/material/stock (señales siempre fiables).
- **Stock** → nunca se ignora: penaliza orden, marca `available=false` y lo comunica.
- **Valores inválidos** → validación de bean + `min ≤ max` → 400; strings desconocidos
  se ignoran (no rompen).
- **Sin coincidencias** → se relajan solo filtros “blandos” (material) de forma explícita;
  si el catálogo no tiene nada, `recommendations: []`.
- **Regresión** → feature aislada en `com.joyeria.gift`; no se toca el esquema ni el
  Asistente IA existente.
