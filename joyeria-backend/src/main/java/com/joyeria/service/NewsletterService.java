package com.joyeria.service;

import com.joyeria.dto.NewsletterRequest;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.NewsletterSubscriber;
import com.joyeria.repository.NewsletterSubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepository;

    public String subscribe(String email) {
        if (subscriberRepository.existsByEmail(email)) {
            NewsletterSubscriber existing = subscriberRepository.findByEmailAndActiveTrue(email).orElse(null);
            if (existing != null) return "Ya estás suscrito a nuestro newsletter";
            throw new IllegalArgumentException("El email ya fue registrado");
        }
        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail(email);
        subscriber.setActive(true);
        subscriberRepository.save(subscriber);
        return "Te has suscrito exitosamente a nuestro newsletter";
    }

    public String unsubscribe(String email) {
        NewsletterSubscriber subscriber = subscriberRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la suscripción"));
        subscriber.setActive(false);
        subscriberRepository.save(subscriber);
        return "Te has desuscrito exitosamente de nuestro newsletter";
    }
}
