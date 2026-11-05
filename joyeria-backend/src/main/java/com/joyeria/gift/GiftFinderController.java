package com.joyeria.gift;

import com.joyeria.dto.ApiResponse;
import com.joyeria.gift.dto.GiftFinderRequest;
import com.joyeria.gift.dto.GiftFinderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint del Smart Gift Finder.
 *
 * <pre>POST /api/gift-finder/recommend</pre>
 *
 * Es público (invitados y usuarios) pero protegido por validación de entrada y
 * rate limiting. Devuelve recomendaciones basadas únicamente en el catálogo real.
 */
@RestController
@RequestMapping("/api/gift-finder")
@RequiredArgsConstructor
public class GiftFinderController {

    private final GiftFinderService giftFinderService;

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<GiftFinderResponse>> recommend(
            @Valid @RequestBody GiftFinderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(giftFinderService.recommend(request)));
    }
}
