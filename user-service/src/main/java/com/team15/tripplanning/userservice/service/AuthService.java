package com.team15.tripplanning.userservice.service;

import com.team15.tripplanning.userservice.dto.AuthResponse;
import com.team15.tripplanning.userservice.dto.LoginRequest;
import com.team15.tripplanning.userservice.dto.RegisterRequest;
import com.team15.tripplanning.userservice.model.Role;
import com.team15.tripplanning.userservice.model.Status;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.observer.MongoEventLogger;
import com.team15.tripplanning.userservice.repository.UserRepository;
import com.team15.tripplanning.userservice.security.JwtService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MongoEventLogger mongoEventLogger;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       MongoEventLogger mongoEventLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mongoEventLogger = mongoEventLogger;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (isBlank(request.name()) || isBlank(request.email())
                || isBlank(request.password()) || isBlank(request.phone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "name, email, password, and phone are required");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already registered");
        }

        if (userRepository.findByPhone(request.phone()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phone already registered");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.TRAVELER);
        user.setStatus(Status.ACTIVE);
        user = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", user.getId());
        mongoEventLogger.onEvent("REGISTERED", payload);

        String token = jwtService.generateToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(token, jwtService.getExpirationMs());
    }

    public AuthResponse login(LoginRequest request) {
        if (isBlank(request.email()) || isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "email and password are required");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", user.getId());
        mongoEventLogger.onEvent("LOGGED_IN", payload);

        String token = jwtService.generateToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(token, jwtService.getExpirationMs());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
