package com.joyeria.config;

import com.joyeria.model.*;
import com.joyeria.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("dev")
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            seedCategories();
        }
        if (materialRepository.count() == 0) {
            seedMaterials();
        }
        if (userRepository.count() == 0) {
            seedUsers();
        }
        if (productRepository.count() == 0) {
            seedProducts();
        }
        log.info("Inicialización de datos completada");
    }

    private void seedCategories() {
        String[][] categories = {
                {"Anillos", "anillos", "Anillos de compromiso, boda y moda para toda ocasión"},
                {"Collares", "collares", "Collares elegantes de oro, plata y perlas"},
                {"Pulseras", "pulseras", "Pulseras y brazaletes de diseño exclusivo"},
                {"Aretes", "aretes", "Aretes y pendientes que realzan tu belleza"},
                {"Dijes", "dijes", "Dijes y colgantes con diseños únicos"},
                {"Sets", "sets", "Conjuntos coordinados para looks completos"},
                {"Joyería personalizada", "joyeria-personalizada", "Creaciones hechas a tu medida"},
                {"Regalos", "regalos", "Ideas de regalo especiales para momentos inolvidables"}
        };

        for (int i = 0; i < categories.length; i++) {
            Category cat = new Category();
            cat.setName(categories[i][0]);
            cat.setSlug(categories[i][1]);
            cat.setDescription(categories[i][2]);
            cat.setDisplayOrder(i + 1);
            cat.setActive(true);
            categoryRepository.save(cat);
        }
        log.info("Categorías inicializadas: {}", categories.length);
    }

    private void seedMaterials() {
        String[][] materials = {
                {"Oro 18K", "oro-18k", "Oro de 18 quilates, el estándar de excelencia en joyería fina"},
                {"Plata 925", "plata-925", "Plata esterlina de 925, brillantez y durabilidad"},
                {"Oro blanco", "oro-blanco", "Oro blanco con acabado elegante y moderno"},
                {"Oro rosa", "oro-rosa", "Oro rosa con tonos cálidos y románticos"},
                {"Piedras preciosas", "piedras-preciosas", "Diamantes, esmeraldas, zafiros y rubíes de la más alta calidad"},
                {"Piedras semipreciosas", "piedras-semipreciosas", "Amatistas, turmalinas, ágatas y más"},
                {"Acero quirúrgico", "acero-quirurgico", "Acero inoxidable hipoalergénico"},
                {"Perlas", "perlas", "Perlas cultivadas de agua dulce y salada"}
        };

        for (String[] material : materials) {
            Material mat = new Material();
            mat.setName(material[0]);
            mat.setSlug(material[1]);
            mat.setDescription(material[2]);
            mat.setActive(true);
            materialRepository.save(mat);
        }
        log.info("Materiales inicializados: {}", materials.length);
    }

    private void seedUsers() {
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("Joyería");
        admin.setEmail("admin@joyeria.com");
        admin.setPhone("+57 300 123 4567");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        User demoUser = new User();
        demoUser.setFirstName("María");
        demoUser.setLastName("García");
        demoUser.setEmail("maria@example.com");
        demoUser.setPhone("+57 310 987 6543");
        demoUser.setPassword(passwordEncoder.encode("user1234"));
        demoUser.setRole(UserRole.USER);
        demoUser.setActive(true);
        userRepository.save(demoUser);

        log.info("Usuarios de prueba creados (perfil dev)");
    }

    private void seedProducts() {
        Map<String, Category> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getSlug, c -> c));
        Map<String, Material> materials = materialRepository.findAll().stream()
                .collect(Collectors.toMap(Material::getSlug, m -> m));

        List<ProductSeed> seeds = List.of(
                new ProductSeed("Anillo Aurora", "anillos", "1850000", "2300000", "JWR-AUR-001", 14, "Dorado", "3.2 g", "15",
                        "Anillo en oro 18K engastado con diamantes certificados y acabado brillante. Una pieza atemporal para los momentos que importan.",
                        "Oro 18 quilates|Diamantes certificados|Acabado brillante|Incluye estuche y certificado",
                        true, true, true, img("photo-1601121141461-9d6647bca1ed"), List.of("oro-18k", "piedras-preciosas")),
                new ProductSeed("Anillo Espiga", "anillos", "980000", null, "JWR-ESP-002", 18, "Plateado", "2.1 g", "14",
                        "Anillo de plata 925 con diseño de espiga realizado a mano. Minimalista, versátil y elegante para el día a día.",
                        "Plata 925|Diseño artesanal|Hipoalergénico|Acabado pulido",
                        false, true, false, img("photo-1535632066927-ab7c9ab60908"), List.of("plata-925")),
                new ProductSeed("Anillo Regente Zafiro", "anillos", "2450000", null, "JWR-REG-003", 7, "Azul", "4.0 g", "16",
                        "Anillo statement en oro 18K con zafiro azul natural y guardas de diamantes. Una pieza de colección.",
                        "Zafiro azul natural|Oro 18 quilates|Guardas de diamantes|Certificado de autenticidad",
                        true, true, false, img("photo-1599643478518-a784e5dc4c8f"), List.of("oro-18k", "piedras-preciosas")),
                new ProductSeed("Collar Estela", "collares", "1200000", "1500000", "JWR-EST-004", 20, "Dorado", "5.5 g", null,
                        "Collar de oro 18K con cadena ajustable y colgante en forma de gota. La pieza perfecta para elevar cualquier look.",
                        "Oro 18 quilates|Cadena ajustable 40-45 cm|Cierre seguridad|Baño de rodio",
                        false, false, true, img("photo-1617038220319-276d3cfab638"), List.of("oro-18k")),
                new ProductSeed("Collar Perlas Serenas", "collares", "890000", "1100000", "JWR-PER-005", 11, "Blanco", "8.0 g", null,
                        "Elegante collar de perlas cultivadas de agua dulce con cierre de oro 18K. Sofisticación en su máxima expresión.",
                        "Perlas cultivadas|Cierre de oro 18K|Doble hebra|Estuche premium",
                        false, true, false, img("photo-1602173574767-37ac01994b2a"), List.of("perlas", "oro-18k")),
                new ProductSeed("Collar Oro Blanco Sol", "collares", "1600000", null, "JWR-SOL-006", 9, "Blanco", "6.2 g", null,
                        "Collar en oro blanco 18K con dije sol pavimentado de diamantes. Brilla con luz propia.",
                        "Oro blanco 18K|Pavé de diamantes|Cadena 45 cm|Garantía de calidad",
                        false, true, false, img("photo-1611591437281-460bfbe1220a"), List.of("oro-blanco", "piedras-preciosas")),
                new ProductSeed("Pulsera Cadena Clásica", "pulseras", "750000", "950000", "JWR-CAD-007", 16, "Dorado", "7.5 g", "17",
                        "Pulsera de cadena clásica en oro 18K con cierre de seguridad. Un básico que nunca pasa de moda.",
                        "Oro 18 quilates|Cierre de seguridad|Baño de rodio|Antialérgica",
                        true, false, false, img("photo-1515562141207-7a88fb7ce338"), List.of("oro-18k")),
                new ProductSeed("Pulsera Mar Océano", "pulseras", "620000", null, "JWR-MAR-008", 22, "Azul", "4.8 g", "16",
                        "Pulsera de plata 925 con piedras semipreciosas tonos océano y dije de ancla. Un recuerdo del mar en tu muñeca.",
                        "Plata 925|Piedras semipreciosas|Ajustable|Envío con estuche",
                        false, false, true, img("photo-1506630448388-4e683c67ddb0"), List.of("plata-925", "piedras-semipreciosas")),
                new ProductSeed("Aretes Brillo Eterno", "aretes", "1100000", "1350000", "JWR-BRI-009", 13, "Dorado", "2.6 g", null,
                        "Aretes de argolla en oro 18K pavimentados de diamantes. Delicados y con un brillo inconfundible.",
                        "Oro 18 quilates|Pavé de diamantes|Tornillo de seguridad|Acabado espejo",
                        true, true, false, img("photo-1601121141461-9d6647bca1ed"), List.of("oro-18k", "piedras-preciosas")),
                new ProductSeed("Aretes Luna Serena", "aretes", "540000", "680000", "JWR-LUN-010", 25, "Plateado", "1.9 g", null,
                        "Aretes de mediacaña en plata 925 con acabado brillante. Ligeros y perfectos para el uso diario.",
                        "Plata 925|Mediacaña brillante|Hipoalergénicos|Proudamente artesanales",
                        false, false, false, img("photo-1573408301185-9146fe634ad0"), List.of("plata-925")),
                new ProductSeed("Dije Estrella Celeste", "dijes", "480000", "600000", "JWR-STE-011", 30, "Dorado", "1.5 g", null,
                        "Dije de estrella en oro 18K perfecto para personalizar tu cadena favorita. Un talismán de buenos deseos.",
                        "Oro 18 quilates|Tamaño 12 mm|Incluye anilla de enlace|Certificado",
                        false, false, true, img("photo-1535632066927-ab7c9ab60908"), List.of("oro-18k")),
                new ProductSeed("Set Realeza", "sets", "3200000", "3900000", "JWR-REA-012", 5, "Dorado", "14.0 g", null,
                        "Set completo de collar, aretes y pulsera en oro 18K con diamantes. Elegancia real para ocasiones especiales.",
                        "3 piezas coordinadas|Oro 18 quilates|Diamantes certificados|Estuche de regalo premium",
                        true, false, false, img("photo-1573408301185-9146fe634ad0"), List.of("oro-18k", "piedras-preciosas")),
                new ProductSeed("Joya Personalizada Recuerdos", "joyeria-personalizada", "1500000", null, "JWR-PER-013", 8, "Dorado", "4.5 g", null,
                        "Pieza única hecha a tu medida con grabado personalizado. Crea un recuerdo eterno con nuestros diseñadores.",
                        "Diseño a la medida|Grabado personalizado|Consultoría de diseño|Certificado de exclusividad",
                        false, true, false, img("photo-1602173574767-37ac01994b2a"), List.of("oro-18k", "plata-925")),
                new ProductSeed("Regalo Especial Corazón", "regalos", "850000", "1000000", "JWR-REG-014", 17, "Dorado", "3.0 g", null,
                        "Dije de corazón en oro 18K con piedra semipreciosa roja. El regalo perfecto para decir lo que sientes.",
                        "Oro 18 quilates|Piedra semipreciosa|Incluye caja de regalo|Tarjeta personalizable",
                        false, false, true, img("photo-1515562141207-7a88fb7ce338"), List.of("oro-18k", "piedras-semipreciosas"))
        );

        for (ProductSeed seed : seeds) {
            createProduct(seed, categories, materials);
        }
        log.info("Productos de demostración creados: {}", seeds.size());
    }

    private void createProduct(ProductSeed s, Map<String, Category> categories, Map<String, Material> materials) {
        Product p = new Product();
        p.setName(s.name());
        p.setSlug(generateSlug(s.name()));
        p.setDescription(s.description());
        p.setPrice(new BigDecimal(s.price()));
        if (s.comparePrice() != null && !s.comparePrice().isBlank()) {
            p.setComparePrice(new BigDecimal(s.comparePrice()));
        }
        p.setSku(s.sku());
        p.setStock(s.stock());
        p.setColor(s.color());
        p.setWeight(s.weight());
        p.setSize(s.size());
        p.setFeatures(s.features());
        p.setCareInstructions("Evitar contacto con perfumes y productos químicos. Guardar en el estuche y limpiar con un paño suave.");
        p.setDeliveryTime("1-3 días hábiles");
        p.setFeatured(s.featured());
        p.setIsNew(s.isNew());
        p.setBestSeller(s.bestSeller());
        p.setActive(true);
        p.setCategory(categories.get(s.category()));
        for (String mSlug : s.materials()) {
            Material m = materials.get(mSlug);
            if (m != null) {
                p.getMaterials().add(m);
            }
        }
        ProductImage img = new ProductImage();
        img.setProduct(p);
        img.setUrl(s.image());
        img.setAlt(s.name());
        img.setIsPrimary(true);
        img.setSortOrder(0);
        p.getImages().add(img);
        productRepository.save(p);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u')
                .replace('ü', 'u').replace('ñ', 'n')
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private String img(String photoId) {
        return "https://images.unsplash.com/" + photoId + "?auto=format&fit=crop&w=800&q=80";
    }

    private record ProductSeed(String name, String category, String price, String comparePrice, String sku,
                               int stock, String color, String weight, String size, String description,
                               String features, boolean featured, boolean isNew, boolean bestSeller,
                               String image, List<String> materials) {}
}