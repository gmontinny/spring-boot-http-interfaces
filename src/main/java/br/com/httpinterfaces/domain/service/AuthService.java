package br.com.httpinterfaces.domain.service;

import br.com.httpinterfaces.application.dto.*;
import br.com.httpinterfaces.domain.model.RefreshToken;
import br.com.httpinterfaces.domain.model.Role;
import br.com.httpinterfaces.domain.model.User;
import br.com.httpinterfaces.domain.repository.RefreshTokenRepository;
import br.com.httpinterfaces.domain.repository.RoleRepository;
import br.com.httpinterfaces.domain.repository.UserRepository;
import br.com.httpinterfaces.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        if (userRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Role ROLE_USER not found"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(accessToken, refreshToken, user.getUsername());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(accessToken, refreshToken, user.getUsername());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("Refresh token expired, please login again");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(stored.getUser().getUsername());
        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = createRefreshToken(stored.getUser());
        refreshTokenRepository.delete(stored);

        return buildAuthResponse(newAccessToken, newRefreshToken, stored.getUser().getUsername());
    }

    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username)
                .ifPresent(refreshTokenRepository::deleteByUser);
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();
        return refreshTokenRepository.save(token).getToken();
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, String username) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .username(username)
                .build();
    }
}
