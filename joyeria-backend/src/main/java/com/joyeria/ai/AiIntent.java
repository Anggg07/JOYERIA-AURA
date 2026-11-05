package com.joyeria.ai;

/**
 * Intención conversacional reconocida por el asistente AURA.
 *
 * <p>Separa explícitamente la <strong>conversación social</strong> (saludos,
 * small talk, agradecimientos) de las <strong>búsquedas comerciales</strong>. La
 * intención se determina <em>antes</em> de decidir si se consulta el catálogo.</p>
 */
public enum AiIntent {

    /** Saludos y despedidas ("hola", "buenas tardes", "adiós"). */
    GREETING,

    /** Conversación social ("¿cómo estás?", "¿qué tal?", "gracias"). */
    SMALL_TALK,

    /** Búsqueda de productos por término, categoría o material. */
    PRODUCT_SEARCH,

    /** Pregunta sobre un producto concreto (normalmente usando contexto previo). */
    PRODUCT_DETAILS,

    /** Búsqueda condicionada por presupuesto. */
    BUDGET_SEARCH,

    /** Búsqueda orientada a regalos. */
    GIFT_ADVICE,

    /** Pregunta sobre el catálogo (categorías, materiales, qué se vende). */
    CATALOG_QUESTION,

    /** Mensaje ambiguo que requiere una pregunta de aclaración. */
    CLARIFICATION,

    /** Solicitud ajena a las capacidades del asistente. */
    OUT_OF_SCOPE
}
