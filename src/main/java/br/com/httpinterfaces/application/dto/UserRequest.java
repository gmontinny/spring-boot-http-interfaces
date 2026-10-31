package br.com.httpinterfaces.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 6) String password,
        Set<String> roles
) {}
