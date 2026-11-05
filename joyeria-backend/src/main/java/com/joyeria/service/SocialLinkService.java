package com.joyeria.service;

import com.joyeria.dto.*;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.SocialLink;
import com.joyeria.repository.SocialLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialLinkService {

    private final SocialLinkRepository socialLinkRepository;

    public List<SocialLinkDTO> getAllLinks() {
        return socialLinkRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(this::mapToDTO).toList();
    }

    public List<SocialLinkDTO> getAllLinksAdmin() {
        return socialLinkRepository.findAll()
                .stream().map(this::mapToDTO).toList();
    }

    public SocialLinkDTO createLink(SocialLinkCreateRequest request) {
        SocialLink link = new SocialLink();
        link.setName(request.getName());
        link.setUrl(request.getUrl());
        link.setIcon(request.getIcon());
        link.setActive(request.getActive());
        link.setSortOrder(request.getSortOrder());
        return mapToDTO(socialLinkRepository.save(link));
    }

    public SocialLinkDTO updateLink(Long id, SocialLinkCreateRequest request) {
        SocialLink link = socialLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enlace no encontrado"));
        if (request.getName() != null) link.setName(request.getName());
        if (request.getUrl() != null) link.setUrl(request.getUrl());
        if (request.getIcon() != null) link.setIcon(request.getIcon());
        if (request.getActive() != null) link.setActive(request.getActive());
        if (request.getSortOrder() != null) link.setSortOrder(request.getSortOrder());
        return mapToDTO(socialLinkRepository.save(link));
    }

    public void deleteLink(Long id) {
        socialLinkRepository.deleteById(id);
    }

    private SocialLinkDTO mapToDTO(SocialLink link) {
        return SocialLinkDTO.builder()
                .id(link.getId())
                .name(link.getName())
                .url(link.getUrl())
                .icon(link.getIcon())
                .active(link.getActive())
                .sortOrder(link.getSortOrder())
                .build();
    }
}
