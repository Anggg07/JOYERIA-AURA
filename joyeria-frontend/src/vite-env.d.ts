/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_AI_ASSISTANT_ENABLED?: string
  readonly VITE_AI_ADMIN_ENABLED?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
