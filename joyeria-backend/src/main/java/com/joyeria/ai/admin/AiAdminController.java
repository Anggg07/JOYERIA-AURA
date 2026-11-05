package com.joyeria.ai.admin;

import com.joyeria.ai.admin.dto.AiAdminRequest;
import com.joyeria.ai.admin.dto.AiAdminResponse;
import com.joyeria.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint del asistente interno del panel de administración.
 *
 * <pre>POST /api/admin/ai/assistant</pre>
 *
 * Requiere rol ADMIN (también está cubierto por la regla {@code /api/admin/**}
 * de {@code SecurityConfig}). Nunca ejecuta acciones: solo responde y, como
 * máximo, propone una acción que el administrador debe confirmar.
 */
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiAdminController {

    private final AiAdminAssistantService aiAdminAssistantService;

    @PostMapping("/assistant")
    public ResponseEntity<ApiResponse<AiAdminResponse>> assistant(
            @Valid @RequestBody AiAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                aiAdminAssistantService.assist(request.getMessage(), request.getHistory())));
    }
}
