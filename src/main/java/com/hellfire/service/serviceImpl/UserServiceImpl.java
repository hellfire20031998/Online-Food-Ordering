package com.hellfire.service.serviceImpl;

import com.hellfire.Config.JwtProvider;
import com.hellfire.model.Address;
import com.hellfire.model.User;
import com.hellfire.repository.UserRepository;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private JwtProvider provider;

    @Override
    public User findUserByJwtToken(String jwt) throws Exception {
     String email=provider.getEmailFromJwtToken(jwt);

     User user=findUserByEmail(email);

     return user;
    }

    @Override
    public User findUserByEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if(user == null){throw  new Exception("User not found");}
        return user;
    }


}

