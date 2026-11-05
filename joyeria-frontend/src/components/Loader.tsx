import { motion } from 'framer-motion'

export default function Loader({ fullScreen = false }: { fullScreen?: boolean }) {
  return (
    <div className={`flex items-center justify-center ${fullScreen ? 'h-screen' : 'py-20'}`}>
      <motion.div className="flex gap-2" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        {[0, 1, 2].map(i => (
          <motion.div key={i} className="w-2 h-2 rounded-full bg-[#C9A227]"
            animate={{ y: [0, -12, 0] }} transition={{ duration: 0.6, repeat: Infinity, delay: i * 0.15 }} />
        ))}
      </motion.div>
    </div>
  )
}
