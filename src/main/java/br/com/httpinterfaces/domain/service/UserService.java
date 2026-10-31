package br.com.httpinterfaces.domain.service;

import br.com.httpinterfaces.application.dto.UserRequest;
import br.com.httpinterfaces.application.dto.UserResponse;
import br.com.httpinterfaces.application.mapper.UserMapper;
import br.com.httpinterfaces.domain.model.Role;
import br.com.httpinterfaces.domain.model.User;
import br.com.httpinterfaces.domain.repository.RoleRepository;
import br.com.httpinterfaces.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    public UserResponse findById(Long id) {
        return userMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.username()))
            throw new IllegalArgumentException("Username already taken: " + request.username());
        if (userRepository.existsByEmail(request.email()))
            throw new IllegalArgumentException("Email already in use: " + request.email());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(resolveRoles(request.roles()))
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = getOrThrow(id);

        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username()))
            throw new IllegalArgumentException("Username already taken: " + request.username());
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email()))
            throw new IllegalArgumentException("Email already in use: " + request.email());

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        if (request.roles() != null && !request.roles().isEmpty())
            user.setRoles(resolveRoles(request.roles()));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id))
            throw new NoSuchElementException("User not found: " + id);
        userRepository.deleteById(id);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty())
            return Set.of(roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("Role ROLE_USER not found")));

        return roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name)))
                .collect(Collectors.toSet());
    }
}
