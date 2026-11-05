import { useId } from 'react'
import { useTheme } from '../contexts/ThemeContext'

interface LogoProps {
  variant?: 'light' | 'dark'
  compact?: boolean
  className?: string
}

export default function Logo({ variant = 'light', compact = false, className = '' }: LogoProps) {
  const id = useId().replace(/:/g, '')
  const g = `${id}-gold`
  const l = `${id}-leaf`
  const { theme } = useTheme()
  // 'dark' variant always sits on a dark panel (footer, parallax); the 'light'
  // variant follows the site theme so the wordmark stays legible in dark mode.
  const onDark = variant === 'dark' || theme === 'dark'
  const txt = onDark ? '#F5F0E8' : '#1E1E1E'
  const sub = onDark ? '#9CA3AF' : '#6B7280'

  if (compact) {
    return (
      <svg viewBox="0 0 200 60" fill="none" xmlns="http://www.w3.org/2000/svg"
        className={className} role="img" aria-label="AURA Fine Artisan Jewelry">
        <defs>
          <linearGradient id={g} x1="8" y1="8" x2="52" y2="52" gradientUnits="userSpaceOnUse">
            <stop stopColor="#C9A227" />
            <stop offset="0.5" stopColor="#F5D06F" />
            <stop offset="1" stopColor="#B8911F" />
          </linearGradient>
          <linearGradient id={l} x1="0" y1="0" x2="0" y2="1">
            <stop stopColor="#7FA082" />
            <stop offset="1" stopColor="#5A7A5D" />
          </linearGradient>
        </defs>
        {/* Outer diamond */}
        <path d="M30 8L50 30 30 52 10 30Z" stroke={`url(#${g})`} strokeWidth="1.8" strokeLinejoin="round" />
        {/* Inner echo */}
        <path d="M30 18L43 30 30 42 17 30Z" stroke={`url(#${g})`} strokeWidth="0.9" opacity="0.45" strokeLinejoin="round" />
        {/* Left leaf */}
        <path d="M19 40Q10 44 6 50Q12 50 18 45Z" fill={`url(#${l})`} opacity="0.82" />
        <path d="M13 43Q18 42 21 39" stroke={`url(#${l})`} strokeWidth="1.1" strokeLinecap="round" fill="none" />
        {/* Right leaf */}
        <path d="M41 20Q50 16 55 10Q49 10 43 15Z" fill={`url(#${l})`} opacity="0.82" />
        <path d="M47 17Q42 18 39 21" stroke={`url(#${l})`} strokeWidth="1.1" strokeLinecap="round" fill="none" />
        {/* AURA wordmark */}
        <text x="114" y="41" textAnchor="middle" fill={txt}
          fontFamily="'Playfair Display', Georgia, serif" fontSize="36" fontWeight="600" letterSpacing="6">
          AURA
        </text>
      </svg>
    )
  }

  return (
    <svg viewBox="0 0 240 160" fill="none" xmlns="http://www.w3.org/2000/svg"
      className={className} role="img" aria-label="AURA Fine Artisan Jewelry">
      <defs>
        <linearGradient id={g} x1="60" y1="10" x2="180" y2="92" gradientUnits="userSpaceOnUse">
          <stop stopColor="#C9A227" />
          <stop offset="0.5" stopColor="#F5D06F" />
          <stop offset="1" stopColor="#B8911F" />
        </linearGradient>
        <linearGradient id={l} x1="0" y1="0" x2="0" y2="1">
          <stop stopColor="#7FA082" />
          <stop offset="1" stopColor="#5A7A5D" />
        </linearGradient>
      </defs>
      {/* Outer diamond */}
      <path d="M120 8L175 50 120 92 65 50Z" stroke={`url(#${g})`} strokeWidth="1.8" />
      {/* Inner echo */}
      <path d="M120 26L152 50 120 74 88 50Z" stroke={`url(#${g})`} strokeWidth="0.9" opacity="0.45" />
      {/* Left leaf */}
      <path d="M88 78Q68 64 54 55Q72 51 86 64Z" fill={`url(#${l})`} opacity="0.82" />
      <path d="M65 50Q76 55 86 64" stroke={`url(#${l})`} strokeWidth="1.1" strokeLinecap="round" fill="none" />
      {/* Right leaf */}
      <path d="M152 22Q172 36 186 45Q170 49 154 36Z" fill={`url(#${l})`} opacity="0.82" />
      <path d="M175 50Q164 45 154 36" stroke={`url(#${l})`} strokeWidth="1.1" strokeLinecap="round" fill="none" />
      {/* AURA wordmark */}
      <text x="120" y="122" textAnchor="middle" fill={txt}
        fontFamily="'Playfair Display', Georgia, serif" fontSize="34" fontWeight="600" letterSpacing="14">
        AURA
      </text>
      {/* Tagline */}
      <text x="120" y="144" textAnchor="middle" fill={sub}
        fontFamily="'Inter', Helvetica, sans-serif" fontSize="8.5" fontWeight="400" letterSpacing="3.5" opacity="0.75">
        FINE ARTISAN JEWELRY
      </text>
    </svg>
  )
}