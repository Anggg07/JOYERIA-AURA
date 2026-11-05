import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Footer from '../components/Footer'
import AiAssistantWidget from '../components/AiAssistantWidget'

export default function MainLayout() {
  const { pathname, hash } = useLocation()

  // Smooth-scroll to in-page anchors (e.g. /#saber-hacer) once the target mounts.
  useEffect(() => {
    if (!hash) return
    const id = decodeURIComponent(hash.slice(1))
    let attempts = 0
    const timer = window.setInterval(() => {
      const el = document.getElementById(id)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' })
        window.clearInterval(timer)
      } else if (++attempts > 40) {
        window.clearInterval(timer)
      }
    }, 25)
    return () => window.clearInterval(timer)
  }, [pathname, hash])

  return (
    <>
      <Navbar />
      <main className="flex-1 mt-[76px]">
        <Outlet />
      </main>
      <Footer />
      <AiAssistantWidget />
    </>
  )
}
