package br.com.httpinterfaces.application.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean enabled,
        LocalDateTime createdAt,
        Set<String> roles
) {}
