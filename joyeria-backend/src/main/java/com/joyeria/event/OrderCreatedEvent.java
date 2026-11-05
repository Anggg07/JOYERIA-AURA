package com.joyeria.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(String email, String firstName, String orderNumber, BigDecimal total) {
}
