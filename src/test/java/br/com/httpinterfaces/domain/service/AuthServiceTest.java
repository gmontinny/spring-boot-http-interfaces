package br.com.httpinterfaces.domain.service;

import br.com.httpinterfaces.application.dto.*;
import br.com.httpinterfaces.domain.model.RefreshToken;
import br.com.httpinterfaces.domain.model.Role;
import br.com.httpinterfaces.domain.model.User;
import br.com.httpinterfaces.domain.repository.RefreshTokenRepository;
import br.com.httpinterfaces.domain.repository.RoleRepository;
import br.com.httpinterfaces.domain.repository.UserRepository;
import br.com.httpinterfaces.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604800000L);
    }

    private User user() {
        return User.builder()
                .id(1L).username("john").email("john@example.com").password("encoded").build();
    }

    private Role userRole() {
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_USER");
        return role;
    }

    private UserDetails userDetails() {
        return org.springframework.security.core.userdetails.User
                .withUsername("john").password("encoded").authorities("ROLE_USER").build();
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole()));
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user());
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails());
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUsername()).isEqualTo("john");
        verify(userRepository).save(any());
    }

    @Test
    void register_shouldThrowWhenUsernameAlreadyTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyInUse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void login_shouldReturnTokensForValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("password123");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user()));
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails());
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void refresh_shouldRotateTokens() {
        User u = user();
        RefreshToken stored = RefreshToken.builder()
                .token("old-refresh")
                .user(u)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(stored));
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails());
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-refresh");

        AuthResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void refresh_shouldThrowWhenTokenExpired() {
        User u = user();
        RefreshToken expired = RefreshToken.builder()
                .token("expired-token")
                .user(u)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token expired");

        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void logout_shouldDeleteRefreshToken() {
        User u = user();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(u));

        authService.logout("john");

        verify(refreshTokenRepository).deleteByUser(u);
    }
}
