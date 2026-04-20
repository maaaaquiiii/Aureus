package com.aureus.platform.security;

import com.aureus.ledger.api.AuthRequest;
import com.aureus.ledger.api.AuthResponse;
import com.aureus.ledger.api.RegisterRequest;
import com.aureus.ledger.domain.User;
import com.aureus.ledger.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;

    @Test
    void login_withValidCredentials_returnsToken() {
        User user = buildUser(1L, "test@email.com", "Macarena", "hashed");
        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("test@email.com", 1L)).thenReturn("jwt-token");
        AuthResponse response = authService.login(new AuthRequest("test@email.com", "password123"));
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@email.com");
        assertThat(response.name()).isEqualTo("Macarena");
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("unknown@email.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new AuthRequest("unknown@email.com", "pass")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        User user = buildUser(1L, "test@email.com", "Macarena", "hashed");
        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(new AuthRequest("test@email.com", "wrongpass")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }

    @Test
    void register_withNewEmail_createsUserAndReturnsToken() {
        when(userRepository.findByEmail("new@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        User saved = buildUser(2L, "new@email.com", "Ana", "hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken("new@email.com", 2L)).thenReturn("jwt-token");
        AuthResponse response = authService.register(new RegisterRequest("new@email.com", "Ana", "password123", "EUR"));
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(2L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withExistingEmail_throwsConflict() {
        User existing = buildUser(1L, "existing@email.com", "Macarena", "hashed");
        when(userRepository.findByEmail("existing@email.com")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> authService.register(new RegisterRequest("existing@email.com", "Otra", "pass123", "EUR")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El email ya está registrado");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withNullCurrency_defaultsToEur() {
        when(userRepository.findByEmail("new@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        User saved = buildUser(3L, "new@email.com", "Test", "hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken(anyString(), anyLong())).thenReturn("token");
        authService.register(new RegisterRequest("new@email.com", "Test", "pass123", null));
        verify(userRepository).save(argThat(u -> "EUR".equals(u.getCurrency())));
    }

    private User buildUser(Long id, String email, String name, String hash) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(hash);
        user.setCurrency("EUR");
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {}
        return user;
    }
}