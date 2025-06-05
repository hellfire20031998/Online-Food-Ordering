package com.hellfire.controller;


import com.hellfire.Config.JwtProvider;
import com.hellfire.model.Cart;
import com.hellfire.model.USER_ROLE;
import com.hellfire.model.User;
import com.hellfire.repository.CartRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.request.loginRequest;
import com.hellfire.response.AuthResponse;
import com.hellfire.service.CustomerUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private CustomerUserDetailsService customerUserDetailsService;
    @Autowired
    private CartRepository  cartRepository;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> createUserHandler(@RequestBody User user) throws Exception {
        System.out.println("Hello from signup controller "+user);
        User isEmailExists=userRepository.findByEmail(user.getEmail());

        if(isEmailExists != null){
            throw new Exception("Email is already registered");
        }
//        User createUser =user;
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);

        Cart cart=new Cart();
        cart.setCustomer(user);
        cartRepository.save(cart);

        UserDetails userDetails = customerUserDetailsService.loadUserByUsername(user.getEmail());

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Successfully registered");
        authResponse.setRole(user.getRole());



        return  new ResponseEntity<>(authResponse, HttpStatus.CREATED);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signinHandler(@RequestBody loginRequest request) throws Exception {

        String username = request.getEmail();
        String password = request.getPassword();

        Authentication authentication = authenticate(username, password);
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // safely extract first role string (e.g., "ADMIN", "ROLE_ADMIN", etc.)
        String roleStr = authorities.isEmpty() ? null : authorities.iterator().next().getAuthority();

        // convert if necessary (e.g., "ROLE_ADMIN" → "ADMIN")
        String cleanRole = roleStr.replace("ROLE_", "");

        String jwt = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Successfully LoggedIn");
        authResponse.setRole(USER_ROLE.valueOf(cleanRole)); // Enum valueOf

        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }


    private Authentication authenticate(String username, String password) {
        UserDetails userDetails=customerUserDetailsService.loadUserByUsername(username);

        if(userDetails==null){
            throw new BadCredentialsException("Invalid username or password");
        }
        if(!passwordEncoder.matches(password,userDetails.getPassword())){
            throw new BadCredentialsException("Invalid password");
        }
       return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @GetMapping("/roles")
    public List<USER_ROLE> getRoles(){

        USER_ROLE admin=USER_ROLE.ADMIN;
        USER_ROLE manager=USER_ROLE.CUSTOMER;
        USER_ROLE member=USER_ROLE.MEMBER;

        List<USER_ROLE> roles = Arrays.asList(admin,manager,member);
        return roles;
    }


}
