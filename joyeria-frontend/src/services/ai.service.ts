import api from './api'
import type { AiAssistantResponse, AiConversationTurn, ApiResponse } from '../types'

/**
 * Asistente de compras con IA. Envía la consulta en lenguaje natural y devuelve
 * un mensaje más productos reales del catálogo (nunca datos inventados).
 *
 * Se reenvían los turnos recientes para que el asistente entienda mensajes de
 * seguimiento ("y en oro?", "más barato").
 */
export const aiService = {
  async ask(message: string, history: AiConversationTurn[] = []): Promise<AiAssistantResponse> {
    const { data } = await api.post<ApiResponse<AiAssistantResponse>>('/ai/assistant', {
      message,
      history,
    })
    return data.data
  },
}

export const aiAssistantEnabled = import.meta.env.VITE_AI_ASSISTANT_ENABLED !== 'false'
