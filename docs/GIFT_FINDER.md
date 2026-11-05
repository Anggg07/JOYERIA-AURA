# Smart Gift Finder

Asistente paso a paso que ayuda a encontrar el regalo ideal mostrando **joyas
reales del catálogo**. El motor de recomendación es **determinista** y puntúa
cada producto según los criterios del usuario; **nunca inventa** productos,
precios, materiales ni disponibilidad: todo dato comercial proviene de
PostgreSQL.

- Auditoría previa: `docs/GIFT_FINDER_AUDIT.md` (decisión de diseño: **Opción D**,
  sin cambios de esquema).
- Endpoint: `POST /api/gift-finder/recommend` (público, con rate limiting).
- Sin variables de entorno nuevas ni dependencias nuevas.

---

## 1. Arquitectura

```
Frontend (/gift-finder) ──POST /api/gift-finder/recommend──▶ GiftFinderController
                                                                    │
                                                                    ▼
                                                            GiftFinderService
                                       ┌────────────────────────────┼───────────────────────────┐
                                       ▼                            ▼                            ▼
                       MaterialRepository              ProductRepository              ProductService
                    (resolver material real)      (consulta real vía searchFiltered)   (mapProducts → DTO)
                                       └────────────────────────────┴───────────────────────────┘
                                                                    ▼
                                                 scoring + orden sobre datos reales
                                                                    ▼
                                         GiftFinderResponse { recommendations[] }
```

Reutiliza infraestructura existente: `TextNormalizer` (normalización de acentos),
`ProductRepository.searchFiltered` (consulta real) y
`ProductService.mapProducts` (mapeo a DTO). No duplica la lógica de productos.

### Paquete backend `com.joyeria.gift`

| Clase | Responsabilidad |
| --- | --- |
| `GiftFinderController` | `POST /api/gift-finder/recommend`, `@Valid`. |
| `GiftFinderService` | Normaliza criterios, consulta real, puntúa, ordena y explica. |
| `dto/GiftFinderRequest` | Criterios (todos opcionales): `recipient`, `occasion`, `style`, `material`, `budgetMin`, `budgetMax`. |
| `dto/GiftRecommendation` | Producto real + `relevance` (nullable) + `explanation`. |
| `dto/GiftFinderResponse` | `recommendations[]` (puede ir vacío). |

---

## 2. Motor de scoring

Cada candidato recibe puntos por cada criterio cumplido. La **disponibilidad
siempre se evalúa**; los criterios no informados se omiten y la relevancia se
recalcula.

```
+25 dentro del presupuesto
+20 coincidencia de material
+20 coincidencia de ocasión (palabras clave reales + categoría afín)
+15 coincidencia de estilo (palabras clave reales)
+15 afinidad con el destinatario (categoría afín documentada)
+10 disponibilidad (stock > 0)
```

- **`relevance`** = `round(100 × score / máximo posible)`. Es orientativa y vale
  `null` si el usuario no eligió ningún criterio.
- **Orden**: `score` desc → disponible antes que agotado → más vendidos.
- **Material**: se resuelve contra `MaterialRepository` y se filtra en la consulta
  real; si no hay coincidencias con ese material, se reintenta sin el filtro de
  material (respetando siempre el presupuesto) para no dejar al usuario sin
  resultados.
- **Stock**: un producto agotado **nunca se oculta**, solo se penaliza en el orden
  (`available=false` y la explicación lo indica).
- **Afinidades** (destinatario/ocasión/estilo) se derivan del texto real del
  catálogo (categoría, nombre, descripción, features, materiales). No son datos
  inventados: solo guían el orden.

---

## 3. Contrato de la API

`POST /api/gift-finder/recommend` — `Content-Type: application/json`

Petición (todos los campos opcionales):

```json
{
  "recipient": "pareja",
  "occasion": "aniversario",
  "style": "elegante",
  "material": "oro",
  "budgetMin": 500000,
  "budgetMax": 1500000
}
```

Respuesta (`ApiResponse<GiftFinderResponse>`):

```json
{
  "success": true,
  "data": {
    "recommendations": [
      {
        "id": 1,
        "name": "Anillo Aurora",
        "slug": "anillo-aurora",
        "price": 1850000,
        "image": "https://images.unsplash.com/…",
        "material": "Oro 18K",
        "materialNames": ["Oro 18K", "Diamantes certificados"],
        "categoryName": "Anillos",
        "available": true,
        "relevance": 86,
        "explanation": "Lo recomendamos porque se ajusta a tu presupuesto y encaja con esa ocasión."
      }
    ]
  },
  "timestamp": "2026-01-01T12:00:00Z"
}
```

- Máximo **8** recomendaciones reales.
- Sin coincidencias → `recommendations: []` (la UI muestra un estado vacío).
- `budgetMin`/`budgetMax` deben ser ≥ 0 y `budgetMin ≤ budgetMax`; en caso
  contrario → `400` (`GlobalExceptionHandler`).

---

## 4. Seguridad

- **Público**: `POST /api/gift-finder/**` está en `permitAll` (`SecurityConfig`);
  disponible para invitados y usuarios, sin datos personales.
- **Rate limiting**: regla `POST|/api/gift-finder/recommend` → **20 / 60 s** por IP
  (`RateLimitFilter`).
- **Validación**: `@Valid` + `@PositiveOrZero` en los presupuestos.
- **Sin alucinaciones**: todos los campos devueltos son de catálogo.
- **Degradación**: si faltan datos, el motor omite criterios y sigue funcionando.

---

## 5. Frontend

| Archivo | Descripción |
| --- | --- |
| `src/pages/GiftFinder.tsx` | Asistente de 5 pasos + resultados (ruta `/gift-finder`). |
| `src/services/gift.service.ts` | `giftService.recommend(criteria)` → `POST /gift-finder/recommend`. |
| `src/App.tsx` | Ruta `gift-finder` bajo `MainLayout`. |
| `src/components/Navbar.tsx` | Enlace "Encuentra el regalo" (escritorio y móvil). |
| `src/types/index.ts` | Tipos `GiftFinderRequest`, `GiftRecommendation`, `GiftFinderResponse`. |

Comportamiento:

- **Mobile-first**, con transiciones de `framer-motion` que respetan
  `useReducedMotion`.
- **5 pasos**: destinatario → ocasión → estilo → presupuesto → material. Cada paso
  ofrece **"No estoy seguro"** para saltarlo y sigue funcionando con filtros
  incompletos. Barra de progreso y resumen de criterios elegidos.
- **Resultados reales**: imagen (`SmartImage`), nombre, categoría, precio
  (`formatPrice`), material, `relevance` ("% coincidencia"), explicación y
  disponibilidad ("Agotado"). "Agregar" al carrito requiere sesión; si no la hay,
  invita a iniciar sesión.
- **URL compartible**: los criterios se reflejan en la query
  (`/gift-finder?occasion=boda&budget=lt500k&material=Oro…`); al abrir un enlace
  con criterios, la búsqueda se ejecuta automáticamente.
- Estados de **carga** (skeleton), **error** (mensaje de respaldo) y **sin
  resultados** (invita a ajustar criterios).

---

## 6. Pruebas

Backend (`joyeria-backend/src/test/java/com/joyeria/gift/`):

- `GiftFinderServiceTest` (10) — presupuesto, material, ocasión, estilo, filtros
  incompletos, sin criterios (relevancia `null`), agotados, sin coincidencias,
  presupuesto inválido (min > max) y negativo.
- `GiftFinderControllerTest` (5) — 200 con recomendaciones, cuerpo vacío,
  presupuesto negativo → 400, min > max → 400, cuerpo malformado → 400.

Frontend:

- `src/services/gift.service.test.ts` — llamada al endpoint.
- `src/pages/GiftFinder.test.tsx` (8) — primer paso, envío de criterios + render,
  estado vacío, agotado, filtros incompletos ("No estoy seguro"), criterios
  compartidos por URL, agregar al carrito, invitado → inicio de sesión.

Verificación ejecutada:

- `mvn -o test` → **159/159** ✅ (15 nuevos, sin regresiones).
- `npm run lint` → 0 errores (solo warnings preexistentes).
- `npm test` → **35/35** ✅ (9 nuevos).
- `npm run build` → build de producción correcto.

---

## 7. Archivos nuevos / modificados

**Backend — nuevos**: `com/joyeria/gift/GiftFinderService.java`,
`com/joyeria/gift/GiftFinderController.java`, `com/joyeria/gift/dto/*`, y sus tests.

**Backend — modificados**: `config/SecurityConfig.java`,
`security/RateLimitFilter.java`.

**Frontend — nuevos**: `pages/GiftFinder.tsx`, `services/gift.service.ts` y sus tests.

**Frontend — modificados**: `App.tsx`, `components/Navbar.tsx`, `types/index.ts`.

**Raíz**: `docs/GIFT_FINDER.md` y `docs/GIFT_FINDER_AUDIT.md`.

---

## 7bis. Decisiones de diseño

- **Sin cambios de esquema** (Opción D de la auditoría): no se añadieron columnas
  `occasion`/`style`/`recipient`/`gender`; el motor deduce afinidades del texto
  real ya almacenado. Ventaja: cero migraciones y cero datos ficticios.
- **Presupuesto siempre respetado** (nunca se relaja); el único filtro que se
  relaja ante ausencia de resultados es el material.
- **Relevancia nullable**: sin criterios de usuario no se muestra un porcentaje
  que podría inducir a error.

---

## 8. Límites y recomendaciones futuras

- **Precisión de ocasión/estilo**: depende del texto real de cada producto; mejorar
  la descripción/features en el panel admin mejora las recomendaciones sin tocar
  código.
- **Afinidades configurables**: hoy son mapas en `GiftFinderService`; a futuro
  podrían externalizarse a configuración (`application.yml`) o a datos.
- **Paginación**: se devuelven 8 recomendaciones; si el catálogo crece, considerar
  "ver más" con paginación real.
