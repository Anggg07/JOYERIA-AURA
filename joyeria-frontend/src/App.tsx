import { Routes, Route, useNavigate } from 'react-router-dom'
import { lazy, Suspense, useEffect } from 'react'
import MainLayout from './layouts/MainLayout'
import AdminLayout from './layouts/AdminLayout'
import ProtectedRoute from './routes/ProtectedRoute'
import AdminRoute from './routes/AdminRoute'
import Loader from './components/Loader'
import CookieConsent from './components/CookieConsent'
import { useAuth } from './contexts/AuthContext'
import { SESSION_EXPIRED_EVENT, SESSION_EXPIRED_MESSAGE } from './utils/sessionEvents'

function SessionWatcher() {
  const navigate = useNavigate()
  const { logout } = useAuth()

  useEffect(() => {
    const handler = () => {
      logout()
      navigate('/login', { state: { message: SESSION_EXPIRED_MESSAGE } })
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, handler)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handler)
  }, [navigate, logout])

  return null
}

const Home = lazy(() => import('./pages/Home'))
const Catalog = lazy(() => import('./pages/Catalog'))
const Personalize = lazy(() => import('./pages/Personalize'))
const GiftFinder = lazy(() => import('./pages/GiftFinder'))
const ProductDetail = lazy(() => import('./pages/ProductDetail'))
const Login = lazy(() => import('./pages/Login'))
const Register = lazy(() => import('./pages/Register'))
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'))
const ResetPassword = lazy(() => import('./pages/ResetPassword'))
const CartPage = lazy(() => import('./pages/CartPage'))
const Checkout = lazy(() => import('./pages/Checkout'))
const OrderConfirmation = lazy(() => import('./pages/OrderConfirmation'))
const SandboxPaymentPage = lazy(() => import('./pages/SandboxPaymentPage'))
const Profile = lazy(() => import('./pages/user/Profile'))
const Orders = lazy(() => import('./pages/user/Orders'))
const Addresses = lazy(() => import('./pages/user/Addresses'))
const Favorites = lazy(() => import('./pages/user/Favorites'))
const SupportPage = lazy(() => import('./pages/SupportPage'))
const FAQ = lazy(() => import('./pages/FAQ'))
const About = lazy(() => import('./pages/About'))
const Privacy = lazy(() => import('./pages/Privacy'))
const Terms = lazy(() => import('./pages/Terms'))
const CookiesPage = lazy(() => import('./pages/CookiesPage'))
const SocialLinks = lazy(() => import('./pages/SocialLinks'))
const NotFound = lazy(() => import('./pages/NotFound'))
const AdminDashboard = lazy(() => import('./pages/admin/Dashboard'))
const AdminProducts = lazy(() => import('./pages/admin/AdminProducts'))
const AdminCategories = lazy(() => import('./pages/admin/AdminCategories'))
const AdminMaterials = lazy(() => import('./pages/admin/AdminMaterials'))
const AdminOrders = lazy(() => import('./pages/admin/AdminOrders'))
const AdminUsers = lazy(() => import('./pages/admin/AdminUsers'))
const AdminReviews = lazy(() => import('./pages/admin/AdminReviews'))
const AdminSupport = lazy(() => import('./pages/admin/AdminSupport'))
const AdminCoupons = lazy(() => import('./pages/admin/AdminCoupons'))
const AdminSocialLinks = lazy(() => import('./pages/admin/AdminSocialLinks'))

export default function App() {
  return (
    <>
      <SessionWatcher />
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Suspense fallback={<Loader />}><Home /></Suspense>} />
          <Route path="productos" element={<Suspense fallback={<Loader />}><Catalog /></Suspense>} />
          <Route path="personaliza" element={<Suspense fallback={<Loader />}><Personalize /></Suspense>} />
          <Route path="gift-finder" element={<Suspense fallback={<Loader />}><GiftFinder /></Suspense>} />
          <Route path="productos/:slug" element={<Suspense fallback={<Loader />}><ProductDetail /></Suspense>} />
          <Route path="login" element={<Suspense fallback={<Loader />}><Login /></Suspense>} />
          <Route path="registro" element={<Suspense fallback={<Loader />}><Register /></Suspense>} />
          <Route path="recuperar-contrasena" element={<Suspense fallback={<Loader />}><ForgotPassword /></Suspense>} />
          <Route path="restablecer-contrasena" element={<Suspense fallback={<Loader />}><ResetPassword /></Suspense>} />
          <Route path="carrito" element={<Suspense fallback={<Loader />}><CartPage /></Suspense>} />
          <Route path="checkout" element={<Suspense fallback={<Loader />}><Checkout /></Suspense>} />
          <Route path="pedido-confirmado/:orderNumber" element={<Suspense fallback={<Loader />}><OrderConfirmation /></Suspense>} />
          <Route path="pago/sandbox" element={<Suspense fallback={<Loader />}><SandboxPaymentPage /></Suspense>} />
          <Route path="cuenta" element={<ProtectedRoute><Suspense fallback={<Loader />}><Profile /></Suspense></ProtectedRoute>} />
          <Route path="cuenta/pedidos" element={<ProtectedRoute><Suspense fallback={<Loader />}><Orders /></Suspense></ProtectedRoute>} />
          <Route path="cuenta/direcciones" element={<ProtectedRoute><Suspense fallback={<Loader />}><Addresses /></Suspense></ProtectedRoute>} />
          <Route path="cuenta/favoritos" element={<ProtectedRoute><Suspense fallback={<Loader />}><Favorites /></Suspense></ProtectedRoute>} />
          <Route path="soporte" element={<Suspense fallback={<Loader />}><SupportPage /></Suspense>} />
          <Route path="faq" element={<Suspense fallback={<Loader />}><FAQ /></Suspense>} />
          <Route path="nosotros" element={<Suspense fallback={<Loader />}><About /></Suspense>} />
          <Route path="privacidad" element={<Suspense fallback={<Loader />}><Privacy /></Suspense>} />
          <Route path="terminos" element={<Suspense fallback={<Loader />}><Terms /></Suspense>} />
          <Route path="cookies" element={<Suspense fallback={<Loader />}><CookiesPage /></Suspense>} />
          <Route path="redes-sociales" element={<Suspense fallback={<Loader />}><SocialLinks /></Suspense>} />
          <Route path="*" element={<Suspense fallback={<Loader />}><NotFound /></Suspense>} />
        </Route>

        <Route path="/admin" element={<AdminRoute><AdminLayout /></AdminRoute>}>
          <Route index element={<Suspense fallback={<Loader />}><AdminDashboard /></Suspense>} />
          <Route path="productos" element={<Suspense fallback={<Loader />}><AdminProducts /></Suspense>} />
          <Route path="categorias" element={<Suspense fallback={<Loader />}><AdminCategories /></Suspense>} />
          <Route path="materiales" element={<Suspense fallback={<Loader />}><AdminMaterials /></Suspense>} />
          <Route path="pedidos" element={<Suspense fallback={<Loader />}><AdminOrders /></Suspense>} />
          <Route path="usuarios" element={<Suspense fallback={<Loader />}><AdminUsers /></Suspense>} />
          <Route path="resenas" element={<Suspense fallback={<Loader />}><AdminReviews /></Suspense>} />
          <Route path="soporte" element={<Suspense fallback={<Loader />}><AdminSupport /></Suspense>} />
          <Route path="cupones" element={<Suspense fallback={<Loader />}><AdminCoupons /></Suspense>} />
          <Route path="redes-sociales" element={<Suspense fallback={<Loader />}><AdminSocialLinks /></Suspense>} />
        </Route>
      </Routes>
      <CookieConsent />
    </>
  )
}