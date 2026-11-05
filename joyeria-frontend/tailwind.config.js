/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          light: 'hsl(var(--primary-light))',
          dark: 'hsl(var(--primary-dark))',
        },
        /* Brand gold — reserved for small accents: eyebrows, 1px borders, icons, hover */
        gold: {
          DEFAULT: '#C9A227',
          light: '#D4AF37',
          soft: '#E7CE84',
          deep: '#A8861B',
        },
        accent: {
          green: 'hsl(var(--accent-green))',
          dark: 'hsl(var(--accent-dark))',
        },
        background: {
          DEFAULT: 'hsl(var(--background))',
          warm: 'hsl(var(--background-warm))',
          light: 'hsl(var(--background-light))',
        },
        surface: {
          DEFAULT: 'hsl(var(--surface))',
          muted: 'hsl(var(--surface-muted))',
          elevated: 'hsl(var(--surface-elevated))',
        },
        foreground: {
          DEFAULT: 'hsl(var(--foreground))',
          muted: 'hsl(var(--foreground-muted))',
          faint: 'hsl(var(--foreground-faint))',
        },
        line: {
          DEFAULT: 'hsl(var(--border))',
        },
        ink: {
          DEFAULT: 'hsl(var(--ink))',
          soft: 'hsl(var(--ink-soft))',
        },
      },
      fontFamily: {
        serif: ['"Playfair Display"', '"Cormorant Garamond"', 'serif'],
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
    },
  },
  plugins: [],
}