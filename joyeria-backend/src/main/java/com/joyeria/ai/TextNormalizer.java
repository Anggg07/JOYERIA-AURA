package com.joyeria.ai;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilidad de normalización de texto para comparaciones insensibles a
 * mayúsculas y acentos (búsqueda de intención, materiales, categorías, etc.).
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutMarks = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutMarks.toLowerCase(Locale.ROOT).trim();
    }
}
