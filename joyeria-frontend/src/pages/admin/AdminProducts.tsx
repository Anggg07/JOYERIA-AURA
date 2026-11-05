import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, Star, BadgePercent, Sparkles, Upload, X } from 'lucide-react'
import { adminService } from '../../services/admin.service'
import { productService } from '../../services/product.service'
import { categoryService } from '../../services/category.service'
import { materialService } from '../../services/material.service'
import { useToast } from '../../contexts/ToastContext'
import type { Product, Category, Material } from '../../types'
import Button from '../../components/Button'
import Modal from '../../components/Modal'
import Skeleton from '../../components/Skeleton'
import Pagination from '../../components/Pagination'
import { formatPrice } from '../../utils/format'
import { getErrorMessage } from '../../utils/errors'

const empty = {
  name: '', description: '', price: '', comparePrice: '', sku: '', stock: '',
  weight: '', dimensions: '', size: '', color: '', careInstructions: '',
  features: '', deliveryTime: '', featured: false, isNew: false, bestSeller: false,
  active: true, categoryId: '', materialIds: [] as number[], imageUrls: '',
}

export default function AdminProducts() {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [materials, setMaterials] = useState<Material[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(false)
  const [editing, setEditing] = useState<Product | null>(null)
  const [form, setForm] = useState<typeof empty>(empty)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const { toast } = useToast()

  useEffect(() => {
    setLoading(true)
    Promise.all([productService.getAll({ page, size: 20 }), categoryService.getAll(), materialService.getAll()])
      .then(([p, c, m]) => {
        setProducts(p.content); setTotalPages(p.totalPages); setCategories(c); setMaterials(m)
      })
      .catch(() => { setProducts([]); setTotalPages(0) })
      .finally(() => setLoading(false))
  }, [page])

  const openNew = () => { setEditing(null); setForm(empty); setModal(true) }

  const openEdit = (p: Product) => {
    setEditing(p)
    setForm({
      name: p.name, description: p.description || '', price: String(p.price),
      comparePrice: p.comparePrice ? String(p.comparePrice) : '', sku: p.sku, stock: String(p.stock),
      weight: p.weight || '', dimensions: p.dimensions || '', size: p.size || '', color: p.color || '',
      careInstructions: p.careInstructions || '', features: p.features || '', deliveryTime: p.deliveryTime || '',
      featured: p.featured, isNew: p.isNew, bestSeller: p.bestSeller, active: p.active,
      categoryId: p.categoryId ? String(p.categoryId) : '', materialIds: p.materialIds || [],
      imageUrls: p.images?.map(i => i.url).join('\n') || '',
    })
    setModal(true)
  }

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    const payload = {
      ...form,
      price: Number(form.price),
      comparePrice: form.comparePrice ? Number(form.comparePrice) : null,
      stock: Number(form.stock),
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      materialIds: form.materialIds,
      imageUrls: form.imageUrls.split('\n').map((s: string) => s.trim()).filter(Boolean),
    }
    try {
      if (editing) await adminService.updateProduct(editing.id, payload)
      else await adminService.createProduct(payload)
      toast(editing ? 'Producto actualizado' : 'Producto creado')
      setModal(false)
      reload()
    } catch (err: unknown) {
      toast(getErrorMessage(err, 'Error al guardar'), 'error')
    } finally { setSaving(false) }
  }

  const remove = async (id: number) => {
    if (!window.confirm('¿Eliminar este producto?')) return
    try { await adminService.deleteProduct(id); toast('Producto eliminado', 'info'); reload() }
    catch (err: unknown) {
      const message = getErrorMessage(err, 'No se pudo eliminar')
      toast(message || 'No se pudo eliminar', 'error')
    }
  }

  const reload = async () => {
    const p = await productService.getAll({ page, size: 20 })
    setProducts(p.content); setTotalPages(p.totalPages)
  }

  const toggleMaterial = (id: number) => {
    setForm((f) => ({
      ...f,
      materialIds: f.materialIds.includes(id) ? f.materialIds.filter((m) => m !== id) : [...f.materialIds, id],
    }))
  }

  const imageList = form.imageUrls.split('\n').map((s) => s.trim()).filter(Boolean)

  const setImages = (list: string[]) => setForm((f) => ({ ...f, imageUrls: list.join('\n') }))

  const uploadImages = async (files: FileList | null) => {
    if (!files || files.length === 0) return
    setUploading(true)
    try {
      const urls: string[] = []
      for (const file of Array.from(files)) {
        urls.push(await adminService.uploadImage(file))
      }
      setForm((f) => ({ ...f, imageUrls: [...imageList, ...urls].join('\n') }))
      toast('Imagen(es) subida(s)')
    } catch (err: unknown) {
      toast(getErrorMessage(err, 'No se pudo subir la imagen'), 'error')
    } finally {
      setUploading(false)
    }
  }

  const input = 'w-full px-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition'
  const label = 'block text-xs font-medium text-foreground-faint mb-1'

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-serif text-2xl font-medium text-foreground">Productos</h1>
          <p className="text-sm text-foreground-faint">{products.length} productos en esta página</p>
        </div>
        <Button onClick={openNew}><Plus size={16} /> Nuevo producto</Button>
      </div>

      {loading ? (
        <Skeleton className="h-80" />
      ) : (
        <div className="bg-surface rounded-lg border border-line/70 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-foreground-faint border-b border-line/70 bg-background-warm">
                  <th className="p-4">Producto</th>
                  <th className="p-4">Precio</th>
                  <th className="p-4">Stock</th>
                  <th className="p-4">Categoría</th>
                  <th className="p-4">Badges</th>
                  <th className="p-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {products.length === 0 ? (
                  <tr><td colSpan={6} className="p-8 text-center text-foreground-faint">No hay productos.</td></tr>
                ) : products.map(p => (
                  <tr key={p.id} className="border-b border-line/40 last:border-0 hover:bg-surface-muted/50">
                    <td className="p-4">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded bg-background-warm overflow-hidden shrink-0">
                          {p.images?.[0]?.url ? <img src={p.images[0].url} alt={p.name} className="w-full h-full object-cover" /> : <div className="w-full h-full flex items-center justify-center text-foreground/20 text-sm">J</div>}
                        </div>
                        <div>
                          <p className="font-medium text-foreground">{p.name}</p>
                          <p className="text-xs text-foreground-faint">{p.sku}</p>
                        </div>
                      </div>
                    </td>
                    <td className="p-4 font-medium">{formatPrice(p.price)}</td>
                    <td className="p-4">
                      <span className={p.stock === 0 ? 'text-red-500' : p.stock < 10 ? 'text-yellow-600' : 'text-foreground-muted'}>{p.stock}</span>
                    </td>
                    <td className="p-4 text-foreground-faint">{p.categoryName || '-'}</td>
                    <td className="p-4">
                      <div className="flex gap-1.5">
                        {p.featured && <span title="Destacado"><Star size={14} className="text-[#C9A227] fill-[#C9A227]" /></span>}
                        {p.bestSeller && <span title="Más vendido"><BadgePercent size={14} className="text-purple-500" /></span>}
                        {p.isNew && <span title="Nuevo"><Sparkles size={14} className="text-accent-green" /></span>}
                        {!p.active && <span className="text-[10px] text-foreground-faint uppercase bg-surface-muted px-1.5 py-0.5 rounded">Inactivo</span>}
                      </div>
                    </td>
                    <td className="p-4">
                      <div className="flex gap-1 justify-end">
                        <button onClick={() => openEdit(p)} aria-label={`Editar ${p.name}`} className="min-w-11 min-h-11 flex items-center justify-center text-foreground-faint hover:text-[#C9A227]"><Pencil size={15} /></button>
                        <button onClick={() => remove(p.id)} aria-label={`Eliminar ${p.name}`} className="min-w-11 min-h-11 flex items-center justify-center text-foreground-faint hover:text-red-500"><Trash2 size={15} /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {totalPages > 1 && <div className="p-4 border-t border-line/70"><Pagination page={page} totalPages={totalPages} onPageChange={setPage} /></div>}
        </div>
      )}

      <Modal isOpen={modal} onClose={() => setModal(false)} title={editing ? 'Editar producto' : 'Nuevo producto'} maxWidth="max-w-3xl">
        <form onSubmit={save} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="col-span-2">
              <label className={label}>Nombre *</label>
              <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required className={input} />
            </div>
            <div className="col-span-2">
              <label className={label}>Descripción</label>
              <textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} rows={3} className={`${input} resize-none`} />
            </div>
            <div>
              <label className={label}>Precio (COP) *</label>
              <input type="number" value={form.price} onChange={e => setForm({ ...form, price: e.target.value })} required min={0} className={input} />
            </div>
            <div>
              <label className={label}>Precio anterior</label>
              <input type="number" value={form.comparePrice} onChange={e => setForm({ ...form, comparePrice: e.target.value })} min={0} className={input} />
            </div>
            <div>
              <label className={label}>SKU</label>
              <input value={form.sku} onChange={e => setForm({ ...form, sku: e.target.value })} className={input} placeholder="JWR-001" />
            </div>
            <div>
              <label className={label}>Stock *</label>
              <input type="number" value={form.stock} onChange={e => setForm({ ...form, stock: e.target.value })} required min={0} className={input} />
            </div>
            <div>
              <label className={label}>Categoría *</label>
              <select value={form.categoryId} onChange={e => setForm({ ...form, categoryId: e.target.value })} required className={input}>
                <option value="">Selecciona...</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div>
              <label className={label}>Tiempo de entrega</label>
              <input value={form.deliveryTime} onChange={e => setForm({ ...form, deliveryTime: e.target.value })} className={input} placeholder="1-3 días hábiles" />
            </div>
            <div>
              <label className={label}>Peso</label>
              <input value={form.weight} onChange={e => setForm({ ...form, weight: e.target.value })} className={input} placeholder="3.5 g" />
            </div>
            <div>
              <label className={label}>Dimensiones</label>
              <input value={form.dimensions} onChange={e => setForm({ ...form, dimensions: e.target.value })} className={input} placeholder="20 x 15 mm" />
            </div>
            <div>
              <label className={label}>Talla</label>
              <input value={form.size} onChange={e => setForm({ ...form, size: e.target.value })} className={input} placeholder="15" />
            </div>
            <div>
              <label className={label}>Color</label>
              <input value={form.color} onChange={e => setForm({ ...form, color: e.target.value })} className={input} placeholder="Dorado" />
            </div>
            <div className="col-span-2">
              <label className={label}>Características (separadas por |)</label>
              <input value={form.features} onChange={e => setForm({ ...form, features: e.target.value })} className={input} placeholder="Oro 18k | Piedra natural | Acabado brillante" />
            </div>
            <div className="col-span-2">
              <label className={label}>Instrucciones de cuidado</label>
              <input value={form.careInstructions} onChange={e => setForm({ ...form, careInstructions: e.target.value })} className={input} />
            </div>
            <div className="col-span-2">
              <label className={label}>Imágenes</label>
              <div className="flex items-center gap-3 mb-2">
                <label className={`cursor-pointer inline-flex items-center gap-2 text-xs px-3 py-2 rounded-lg border border-line hover:border-[#C9A227] text-foreground-muted ${uploading ? 'opacity-60 pointer-events-none' : ''}`}>
                  <Upload size={14} /> {uploading ? 'Subiendo…' : 'Subir imágenes'}
                  <input type="file" accept="image/*" multiple className="hidden" onChange={e => uploadImages(e.target.files)} />
                </label>
                <span className="text-xs text-foreground-faint">o pega URLs abajo</span>
              </div>
              {imageList.length > 0 && (
                <div className="flex flex-wrap gap-2 mb-2">
                  {imageList.map((url, i) => (
                    <div key={`${url}-${i}`} className="relative w-16 h-16 rounded overflow-hidden border border-line/70 group">
                      <img src={url} alt="" className="w-full h-full object-cover" />
                      <button type="button" onClick={() => setImages(imageList.filter((_, idx) => idx !== i))}
                        className="absolute top-0.5 right-0.5 bg-black/60 text-white rounded-full w-5 h-5 flex items-center justify-center opacity-0 group-hover:opacity-100 transition" title="Quitar">
                        <X size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
              <textarea value={form.imageUrls} onChange={e => setForm({ ...form, imageUrls: e.target.value })} rows={2} className={`${input} resize-none font-mono text-xs`} placeholder="https://..." />
            </div>
            <div className="col-span-2">
              <label className={label}>Materiales</label>
              <div className="flex flex-wrap gap-2">
                {materials.map(m => (
                  <button key={m.id} type="button" onClick={() => toggleMaterial(m.id)}
                    className={`px-3 py-1.5 rounded-full border text-xs transition ${form.materialIds.includes(m.id) ? 'bg-[#C9A227] text-white border-[#C9A227]' : 'border-line text-foreground-muted hover:border-[#C9A227]'}`}>
                    {m.name}
                  </button>
                ))}
              </div>
            </div>
            <div className="col-span-2 grid grid-cols-2 md:grid-cols-4 gap-2 text-xs text-foreground-muted">
              {([
                { key: 'featured' as const, label: 'Destacado' },
                { key: 'isNew' as const, label: 'Nuevo' },
                { key: 'bestSeller' as const, label: 'Más vendido' },
                { key: 'active' as const, label: 'Activo' },
              ]).map(t => (
                <label key={t.key} className="flex items-center gap-2 bg-background-warm px-3 py-2 rounded-lg">
                  <input type="checkbox" checked={!!form[t.key]} onChange={e => setForm({ ...form, [t.key]: e.target.checked })} className="accent-[#C9A227]" />
                  {t.label}
                </label>
              ))}
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="ghost" onClick={() => setModal(false)}>Cancelar</Button>
            <Button type="submit" loading={saving} disabled={uploading} className="flex-1">{editing ? 'Guardar cambios' : 'Crear producto'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}