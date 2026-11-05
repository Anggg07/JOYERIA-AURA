import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Instagram, Facebook, Twitter, Youtube, Linkedin, ExternalLink, Link2 } from 'lucide-react'
import { socialService } from '../services/misc.service'
import type { SocialLink } from '../types'
import Skeleton from '../components/Skeleton'
import useDocumentTitle from '../hooks/useDocumentTitle'

const icons: Record<string, React.ComponentType<{ size?: number | string }>> = {
  instagram: Instagram, facebook: Facebook, twitter: Twitter, youtube: Youtube, linkedin: Linkedin,
}

export default function SocialLinks() {
  const [links, setLinks] = useState<SocialLink[]>([])
  const [loading, setLoading] = useState(true)

  useDocumentTitle('Redes sociales')

  useEffect(() => {
    socialService.getAll().then(l => { setLinks(l); setLoading(false) }).catch(() => setLoading(false))
  }, [])

  if (loading) return <div className="max-w-3xl mx-auto px-4 py-16"><Skeleton className="h-64" /></div>

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div className="text-center mb-12">
        <Link2 size={40} className="mx-auto mb-4 text-[#C9A227]" />
        <h1 className="font-serif text-3xl font-medium text-foreground mb-3">Síguenos en redes sociales</h1>
        <p className="text-foreground-faint">Descubre las últimas colecciones, procesos artesanales y promociones exclusivas</p>
      </div>

      {links.length === 0 ? (
        <p className="text-center text-foreground-faint">Pronto estaremos disponibles en más plataformas.</p>
      ) : (
        <div className="grid sm:grid-cols-2 gap-4">
          {links.map((link, i) => {
            const Icon = icons[link.icon?.toLowerCase() || ''] || Link2
            return (
              <motion.a
                key={link.id}
                href={link.url}
                target="_blank"
                rel="noopener noreferrer"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.05 }}
                className="bg-surface border border-line/70 rounded-lg p-6 hover:border-[#C9A227]/40 hover:shadow-sm transition flex items-center gap-4 group">
                <div className="w-12 h-12 rounded-full bg-[#C9A227]/10 flex items-center justify-center text-[#C9A227]">
                  <Icon size={22} />
                </div>
                <div className="flex-1">
                  <p className="font-medium text-foreground">{link.name}</p>
                  <p className="text-xs text-foreground-faint truncate">{link.url.replace(/^https?:\/\//, '')}</p>
                </div>
                <ExternalLink size={18} className="text-foreground/20 group-hover:text-[#C9A227] transition" />
              </motion.a>
            )
          })}
        </div>
      )}
    </div>
  )
}