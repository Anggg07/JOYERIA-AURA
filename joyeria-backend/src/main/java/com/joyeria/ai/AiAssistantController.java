package com.joyeria.ai;

import com.joyeria.ai.dto.AiAssistantRequest;
import com.joyeria.ai.dto.AiAssistantResponse;
import com.joyeria.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint del asistente de compras con IA.
 *
 * <pre>POST /api/ai/assistant</pre>
 *
 * Es público (disponible para invitados y usuarios) pero protegido por la
 * validación de entrada y el rate limiting global. Nunca expone ni requiere
 * datos personales del usuario.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/assistant")
    public ResponseEntity<ApiResponse<AiAssistantResponse>> assistant(
            @Valid @RequestBody AiAssistantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                aiAssistantService.assist(request.getMessage(), request.getHistory())));
    }
}
