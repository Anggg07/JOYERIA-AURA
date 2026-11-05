import api from './api'
import type { AiAdminResponse, AiConversationTurn, ApiResponse } from '../types'

/**
 * Asistente interno del panel de administración. Responde con datos reales de
 * la tienda (solo lectura) y, cuando aplica, propone una acción que el
 * administrador debe confirmar. Requiere rol ADMIN (lo valida el backend).
 */
export const aiAdminService = {
  async ask(message: string, history: AiConversationTurn[] = []): Promise<AiAdminResponse> {
    const { data } = await api.post<ApiResponse<AiAdminResponse>>('/admin/ai/assistant', { message, history })
    return data.data
  },
}

export const aiAdminEnabled = import.meta.env.VITE_AI_ADMIN_ENABLED !== 'false'
