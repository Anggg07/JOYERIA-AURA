package com.joyeria.service;

import com.joyeria.dto.MaterialCreateRequest;
import com.joyeria.dto.MaterialDTO;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.Material;
import com.joyeria.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;

    public List<MaterialDTO> getAllMaterials() {
        return materialRepository.findByActiveTrue()
                .stream().map(this::mapToDTO).toList();
    }

    public MaterialDTO getMaterialById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material no encontrado"));
        return mapToDTO(material);
    }

    public MaterialDTO createMaterial(MaterialCreateRequest request) {
        if (materialRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("El material ya existe");
        }
        Material material = new Material();
        material.setName(request.getName());
        material.setSlug(generateSlug(request.getName()));
        material.setDescription(request.getDescription());
        material.setImage(request.getImage());
        material.setActive(request.getActive() != null ? request.getActive() : true);
        return mapToDTO(materialRepository.save(material));
    }

    public MaterialDTO updateMaterial(Long id, MaterialCreateRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material no encontrado"));
        if (request.getName() != null) material.setName(request.getName());
        if (request.getDescription() != null) material.setDescription(request.getDescription());
        if (request.getImage() != null) material.setImage(request.getImage());
        if (request.getActive() != null) material.setActive(request.getActive());
        return mapToDTO(materialRepository.save(material));
    }

    public void deleteMaterial(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material no encontrado"));
        material.setActive(false);
        materialRepository.save(material);
    }

    private MaterialDTO mapToDTO(Material material) {
        return MaterialDTO.builder()
                .id(material.getId())
                .name(material.getName())
                .slug(material.getSlug())
                .description(material.getDescription())
                .image(material.getImage())
                .active(material.getActive())
                .createdAt(material.getCreatedAt())
                .build();
    }

    private String generateSlug(String name) {
        String slug = name.toLowerCase()
                .replaceAll("[áàäâ]", "a").replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i").replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u").replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
        if (materialRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return slug;
    }
}
