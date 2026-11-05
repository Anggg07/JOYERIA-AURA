package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressDTO>>> getAddresses(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(addressService.getUserAddresses(authentication.getName())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressDTO>> createAddress(
            Authentication authentication, @Valid @RequestBody AddressCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Dirección creada", addressService.createAddress(authentication.getName(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressDTO>> updateAddress(
            Authentication authentication,
            @PathVariable Long id, @Valid @RequestBody AddressCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Dirección actualizada",
                addressService.updateAddress(authentication.getName(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            Authentication authentication, @PathVariable Long id) {
        addressService.deleteAddress(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Dirección eliminada", null));
    }
}
