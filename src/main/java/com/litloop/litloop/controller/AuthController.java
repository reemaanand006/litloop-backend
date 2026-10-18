
package com.litloop.litloop.controller;
import org.springframework.security.core.Authentication;
import com.litloop.litloop.entity.User;
import com.litloop.litloop.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    public record SignupRequest(String name, String email, String password) {}
    public record LoginRequest(String email, String password) {}

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(
            @RequestBody SignupRequest request) {

        if (request.name() == null || request.name().isBlank()
                || request.email() == null || request.password() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Please fill in all fields."));
        }

        String name = request.name().trim();
        String email = request.email().trim().toLowerCase();

        if (name.length() > 100 || email.length() > 150
                || !Pattern.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                email)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Please enter valid details."));
        }

        if (request.password().length() < 8
                || request.password().length() > 72) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Password must be between 8 and 72 characters."));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message",
                            "An account with this email already exists."));
        }

        User user = new User(
                name, email, passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("message",
                        "Account created successfully. You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        if (request.email() == null || request.password() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Email and password are required."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim().toLowerCase(),
                            request.password()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context, httpRequest, httpResponse);

            return ResponseEntity.ok(
                    Map.of("message", "Login successful!"));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message",
                            "Invalid email or password."));
        }
    }
@GetMapping("/me")
public ResponseEntity<Map<String, String>> currentUser(
        Authentication authentication) {

    if (authentication == null
            || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("message", "Please log in."));
    }

    return userRepository.findByEmail(authentication.getName())
            .map(user -> ResponseEntity.ok(
                    Map.of(
                            "name", user.getName(),
                            "email", user.getEmail()
                    )))
            .orElseGet(() -> ResponseEntity.status(
                    HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Please log in.")));
}
}
