package com.joyeria.ai;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extractor determinista de intención. Funciona <strong>sin IA</strong>: es la
 * base del asistente y el mecanismo de respaldo cuando el proveedor no está
 * disponible. No consulta la base de datos ni conoce productos reales.
 */
@Component
public class SearchIntentParser {

    private static final List<String> MATERIALS = List.of(
            "oro blanco", "oro rosa", "oro amarillo", "plata esterlina", "acero inoxidable",
            "plata", "oro", "acero", "titanio", "platino", "bronce", "cuero",
            "diamante", "diamantes", "esmeralda", "esmeraldas", "rubi", "rubies",
            "zafiro", "zafiros", "perla", "perlas", "circonia", "circonias",
            "cuarzo", "amatista", "onix", "topacio");

    private static final List<String> CATEGORIES = List.of(
            "anillo", "anillos", "collar", "collares", "cadena", "cadenas",
            "pulsera", "pulseras", "brazalete", "brazaletes", "arete", "aretes",
            "dije", "dijes", "colgante", "colgantes", "pendiente", "pendientes",
            "gargantilla", "tobillera", "broche", "broches", "sortija", "reloj",
            "relojes", "piercing", "conjunto", "joya", "joyas");

    private static final List<String> COLORS = List.of(
            "dorado", "dorada", "plateado", "plateada", "negro", "negra",
            "blanco", "blanca", "rojo", "roja", "azul", "verde", "rosa",
            "rosado", "morado", "violeta", "gris");

    private static final List<String> STYLES = List.of(
            "elegante", "elegancia", "casual", "moderno", "moderna", "clasico",
            "clasica", "minimalista", "vintage", "bohemio", "bohemia", "romantico",
            "romantica", "lujo", "lujoso", "lujosa", "sofisticado", "sofisticada",
            "discreto", "discreta", "delicado", "delicada", "llamativo");

    private static final List<String> OCCASIONS = List.of(
            "regalo", "regalar", "obsequio", "sorpresa", "boda", "matrimonio",
            "aniversario", "cumpleanos", "graduacion", "compromiso", "navidad",
            "san valentin", "dia de la madre", "dia del padre", "dia de la mujer",
            "primera comunion");

    private static final List<String> MALE = List.of(
            "hombre", "masculino", "caballero", "novio", "esposo", "marido", "papa", "hijo", "abuelo");

    private static final List<String> FEMALE = List.of(
            "mujer", "femenino", "dama", "novia", "esposa", "mama", "hija", "abuela");

    private static final Set<String> STOPWORDS = Set.of(
            "para", "unas", "unos", "una", "uno", "con", "sin", "del", "las", "los",
            "que", "por", "mas", "menos", "algo", "busco", "quiero", "necesito", "tengo",
            "presupuesto", "color", "talla", "modelo", "tipo", "estilo", "muy", "poco",
            "entre", "sobre", "desde", "hasta", "mayor", "menor", "maximo", "minimo",
            "mil", "millon", "millones", "disponible", "disponibles", "stock", "precio",
            "precios", "comprar", "buscando", "busque", "algun", "alguna", "alguno",
            "aqui", "esta", "este", "esos", "esas", "cual", "cuando", "donde");

    /**
     * Términos ya capturados como campos estructurados (categoría, material,
     * color, estilo, ocasión o género). Se excluyen de las palabras clave para
     * no duplicar su puntuación.
     */
    private static final Set<String> STRUCTURED_TERMS = buildStructuredTerms();

    private static final Pattern BETWEEN = Pattern.compile(
            "entre\\s+\\$?\\s*([\\d.,]+)\\s*(mil|millon(?:es)?|k)?\\s*(?:y|a)\\s*\\$?\\s*([\\d.,]+)\\s*(mil|millon(?:es)?|k)?");

    private static final Pattern MAX_PRICE = Pattern.compile(
            "(?:menos de|menor a|menor de|maximo|maxima|hasta|no mas de|por debajo de|" +
            "inferior a|presupuesto(?: de)?|dentro de|no supera(?:r)?)\\s+\\$?\\s*([\\d.,]+)\\s*(mil|millon(?:es)?|k)?");

    private static final Pattern MIN_PRICE = Pattern.compile(
            "(?:mas de|mayor a|mayor de|minimo|minima|desde|a partir de|arriba de|" +
            "superior a|no menos de)\\s+\\$?\\s*([\\d.,]+)\\s*(mil|millon(?:es)?|k)?");

    private static final Pattern ANY_PRICE = Pattern.compile(
            "\\$?\\s*([\\d][\\d.,]*)\\s*(mil|millon(?:es)?|k)?");

    /**
     * Monto mínimo para aceptar un número suelto (sin palabras de presupuesto)
     * como tope de precio. Evita que cifras que no son dinero —como la pureza
     * de la plata (925), el quilataje del oro (18k) o el tamaño— se interpreten
     * como presupuesto.
     */
    private static final BigDecimal MIN_BARE_BUDGET = new BigDecimal("50000");

    /** Cota superior de cordura para cualquier precio sugerido por la IA. */
    private static final BigDecimal MAX_SANE_PRICE = new BigDecimal("1000000000");

    public SearchIntent parse(String rawMessage) {
        SearchIntent intent = new SearchIntent();
        if (rawMessage == null || rawMessage.isBlank()) {
            return intent;
        }
        String norm = TextNormalizer.normalize(rawMessage);

        extractPrices(norm, intent);
        intent.setMaterial(firstMatch(norm, MATERIALS));
        intent.setCategory(firstMatch(norm, CATEGORIES));
        intent.setColor(firstMatch(norm, COLORS));
        intent.setStyle(firstMatch(norm, STYLES));
        intent.setOccasion(firstMatch(norm, OCCASIONS));
        intent.setGender(detectGender(norm));
        intent.setGift(norm.contains("regalo") || norm.contains("regalar")
                || norm.contains("obsequio") || norm.contains("sorpresa"));
        intent.setInStockOnly(norm.contains("disponible") || norm.contains("disponibles")
                || norm.contains("en stock"));
        intent.setKeywords(extractKeywords(norm));
        return intent;
    }

    /**
     * Combina la intención determinista con la propuesta por la IA, descartando
     * valores que no existan en los catálogos reales (protección anti-alucinación).
     */
    public SearchIntent merge(SearchIntent base, SearchIntent ai,
                              Set<String> knownCategories, Set<String> knownMaterials) {
        if (ai == null) {
            return base;
        }
        SearchIntent out = new SearchIntent();
        out.setCategory(firstNonNull(validateAgainst(ai.getCategory(), knownCategories), base.getCategory()));
        out.setMaterial(firstNonNull(validateAgainst(ai.getMaterial(), knownMaterials), base.getMaterial()));
        out.setMinPrice(firstNonNull(sanitizePrice(ai.getMinPrice()), base.getMinPrice()));
        out.setMaxPrice(firstNonNull(sanitizePrice(ai.getMaxPrice()), base.getMaxPrice()));
        out.setColor(firstNonNull(ai.getColor(), base.getColor()));
        out.setStyle(firstNonNull(ai.getStyle(), base.getStyle()));
        out.setOccasion(firstNonNull(ai.getOccasion(), base.getOccasion()));
        out.setGender(firstNonNull(ai.getGender(), base.getGender()));
        out.setGift(ai.isGift() || base.isGift());
        out.setInStockOnly(ai.isInStockOnly() || base.isInStockOnly());
        Set<String> keywords = new LinkedHashSet<>(base.getKeywords());
        if (ai.getKeywords() != null) {
            keywords.addAll(ai.getKeywords());
        }
        out.setKeywords(keywords);
        return out;
    }

    private void extractPrices(String norm, SearchIntent intent) {
        Matcher between = BETWEEN.matcher(norm);
        if (between.find()) {
            intent.setMinPrice(toAmount(between.group(1), between.group(2)));
            intent.setMaxPrice(toAmount(between.group(3), between.group(4)));
            return;
        }
        Matcher max = MAX_PRICE.matcher(norm);
        if (max.find()) {
            intent.setMaxPrice(toAmount(max.group(1), max.group(2)));
        }
        Matcher min = MIN_PRICE.matcher(norm);
        if (min.find()) {
            intent.setMinPrice(toAmount(min.group(1), min.group(2)));
        }
        if (intent.getMaxPrice() == null && intent.getMinPrice() == null) {
            Matcher any = ANY_PRICE.matcher(norm);
            if (any.find()) {
                BigDecimal amount = toAmount(any.group(1), any.group(2));
                if (amount != null && amount.compareTo(MIN_BARE_BUDGET) >= 0) {
                    intent.setMaxPrice(amount);
                }
            }
        }
    }

    private BigDecimal toAmount(String number, String suffix) {
        if (number == null) {
            return null;
        }
        String cleaned = number.replace(".", "").replace(",", "");
        if (cleaned.isBlank()) {
            return null;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
        if (suffix != null) {
            String s = suffix.toLowerCase(Locale.ROOT);
            if (s.startsWith("mil") || s.equals("k")) {
                value = value.multiply(BigDecimal.valueOf(1_000));
            } else if (s.startsWith("millon")) {
                value = value.multiply(BigDecimal.valueOf(1_000_000));
            }
        }
        return value;
    }

    private String detectGender(String norm) {
        boolean male = MALE.stream().anyMatch(term -> containsWord(norm, term));
        boolean female = FEMALE.stream().anyMatch(term -> containsWord(norm, term));
        if (male && female) {
            return "unisex";
        }
        if (female) {
            return "mujer";
        }
        if (male) {
            return "hombre";
        }
        return null;
    }

    private Set<String> extractKeywords(String norm) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : norm.split("[^a-z0-9]+")) {
            if (token.length() < 3 || STOPWORDS.contains(token) || STRUCTURED_TERMS.contains(token)
                    || token.matches("\\d+")) {
                continue;
            }
            keywords.add(token);
            if (keywords.size() >= 6) {
                break;
            }
        }
        return keywords;
    }

    private static Set<String> buildStructuredTerms() {
        Set<String> terms = new LinkedHashSet<>();
        List<List<String>> lexicons = List.of(MATERIALS, CATEGORIES, COLORS, STYLES, OCCASIONS, MALE, FEMALE);
        for (List<String> lexicon : lexicons) {
            for (String entry : lexicon) {
                for (String token : TextNormalizer.normalize(entry).split("[^a-z0-9]+")) {
                    if (!token.isBlank()) {
                        terms.add(token);
                    }
                }
            }
        }
        return terms;
    }

    private String firstMatch(String norm, List<String> terms) {
        for (String term : terms) {
            if (containsWord(norm, term)) {
                return term;
            }
        }
        return null;
    }

    private boolean containsWord(String text, String term) {
        return Pattern.compile("(?:^|[^a-z0-9])" + Pattern.quote(term) + "(?:$|[^a-z0-9])")
                .matcher(text)
                .find();
    }

    private String validateAgainst(String value, Set<String> known) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (known == null || known.isEmpty()) {
            return value.trim();
        }
        String needle = TextNormalizer.normalize(value);
        return known.stream()
                .filter(knownName -> {
                    String candidate = TextNormalizer.normalize(knownName);
                    return candidate.equals(needle) || candidate.contains(needle) || needle.contains(candidate);
                })
                .findFirst()
                .orElse(null);
    }

    private String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private BigDecimal sanitizePrice(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.compareTo(MAX_SANE_PRICE) > 0) {
            return null;
        }
        return value;
    }
}
