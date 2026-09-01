package com.hellfire.controller;

import com.hellfire.config.JwtProvider;
import com.hellfire.exceptions.EmailAlreadyRegisteredException;
import com.hellfire.model.Cart;
import com.hellfire.model.User;
import com.hellfire.model.UserRole;
import com.hellfire.repository.CartRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.request.LoginRequest;
import com.hellfire.request.SignupRequest;
import com.hellfire.response.AuthResponse;
import com.hellfire.service.CustomerUserDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    /** Roles a user may choose at signup. Staff roles are assigned by a restaurant owner. */
    private static final List<UserRole> SIGNUP_ROLES = List.of(UserRole.CUSTOMER, UserRole.ADMIN);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomerUserDetailsService customerUserDetailsService;
    private final CartRepository cartRepository;

    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<AuthResponse> createUserHandler(@Valid @RequestBody SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new EmailAlreadyRegisteredException("Email is already registered");
        }

        UserRole role = request.getRole() == null ? UserRole.CUSTOMER : request.getRole();
        if (!SIGNUP_ROLES.contains(role)) {
            throw new IllegalArgumentException("Role " + role + " cannot be chosen at signup");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        Cart cart = new Cart();
        cart.setCustomer(user);
        cartRepository.save(cart);

        UserDetails userDetails = customerUserDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String jwt = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse(jwt, "Successfully registered", user.getRole());
        return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signinHandler(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticate(request.getEmail(), request.getPassword());

        UserRole role = authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> UserRole.valueOf(authority.getAuthority().replace("ROLE_", "")))
                .orElse(UserRole.CUSTOMER);

        String jwt = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse(jwt, "Successfully logged in", role);
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

    private Authentication authenticate(String username, String password) {
        UserDetails userDetails = customerUserDetailsService.loadUserByUsername(username);
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(), null, userDetails.getAuthorities());
    }

    @GetMapping("/roles")
    public List<UserRole> getRoles() {
        return SIGNUP_ROLES;
    }

    @GetMapping("/restaurant/roles")
    public List<UserRole> getRestaurantRoles() {
        return List.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.MEMBER);
    }
}
