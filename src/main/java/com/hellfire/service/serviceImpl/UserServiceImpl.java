package com.hellfire.service.serviceImpl;

import com.hellfire.config.JwtProvider;
import com.hellfire.exceptions.AccountBlockedException;
import com.hellfire.model.User;
import com.hellfire.repository.UserRepository;
import com.hellfire.service.CustomerUserDetailsService;
import com.hellfire.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * Resolves the acting user from the JWT. Every authenticated controller goes through here,
     * so a BLOCKED account is rejected on its next request even though its token is still valid.
     */
    @Override
    public User findUserByJwtToken(String jwt) throws Exception {
        String email = jwtProvider.getEmailFromJwtToken(jwt);
        User user = findUserByEmail(email);
        if (user.isBlocked()) {
            throw new AccountBlockedException(CustomerUserDetailsService.BLOCKED_MESSAGE);
        }
        return user;
    }

    @Override
    public User findUserByEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }
}
