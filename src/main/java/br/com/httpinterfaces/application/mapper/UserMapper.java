package br.com.httpinterfaces.application.mapper;

import br.com.httpinterfaces.application.dto.UserResponse;
import br.com.httpinterfaces.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet())
        );
    }
}
