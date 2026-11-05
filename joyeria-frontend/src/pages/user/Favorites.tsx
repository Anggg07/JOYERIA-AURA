import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Heart } from 'lucide-react'
import { userService } from '../../services/user.service'
import type { Product } from '../../types'
import UserNav from '../../components/UserNav'
import ProductCard from '../../components/ProductCard'
import Button from '../../components/Button'
import { useToast } from '../../contexts/ToastContext'
import { useCart } from '../../contexts/CartContext'
import { getErrorMessage } from '../../utils/errors'
import useDocumentTitle from '../../hooks/useDocumentTitle'

export default function Favorites() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const { toast } = useToast()
  const { addItem } = useCart()

  useDocumentTitle('Mis favoritos')

  useEffect(() => { load() }, [])

  const load = async () => {
    setLoading(true)
    try {
      const d = await userService.getFavorites(0, 50)
      setProducts(d.content)
    } catch { setProducts([]) }
    finally { setLoading(false) }
  }

  const remove = async (id: number) => {
    await userService.toggleFavorite(id)
    toast('Eliminado de favoritos', 'info')
    load()
  }

  const addToCart = async (id: number) => {
    try {
      await addItem(id)
      toast('Agregado al carrito')
    } catch (err) {
      toast(getErrorMessage(err, 'No se pudo agregar al carrito'), 'error')
    }
  }

  if (loading) {
    return <div className="max-w-6xl mx-auto px-4 py-10"><div className="flex items-center justify-center py-20"><div className="w-10 h-10 border-2 border-[#C9A227]/30 border-t-[#C9A227] rounded-full animate-spin" /></div></div>
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="font-serif text-2xl font-medium text-foreground mb-6">Mis favoritos</h1>
      <UserNav />
      {products.length === 0 ? (
        <div className="bg-surface rounded-lg border border-line/70 p-12 text-center">
          <Heart size={40} className="mx-auto mb-3 text-foreground/20" />
          <p className="text-foreground-faint mb-4">No tienes joyas favoritas aún</p>
          <Link to="/productos"><Button>Explorar productos</Button></Link>
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
          {products.map(p => (
            <div key={p.id} className="relative">
              <ProductCard product={p} onAddToCart={addToCart} />
              <button onClick={() => remove(p.id)}
                className="absolute top-2 right-2 z-10 bg-surface p-2 rounded-full shadow text-red-500 hover:bg-red-50 transition"
                title="Quitar de favoritos">
                <Heart size={16} fill="currentColor" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}