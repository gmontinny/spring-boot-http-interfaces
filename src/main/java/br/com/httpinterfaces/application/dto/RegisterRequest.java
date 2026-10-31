package br.com.httpinterfaces.application.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank @Size(min = 3, max = 100)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8, max = 100)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$",
             message = "Password must have uppercase, digit and special character")
    private String password;
}
