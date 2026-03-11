package com.hellfire.controller;


import com.hellfire.model.User;
import com.hellfire.user.dto.UserDto;
import com.hellfire.user.mapper.UserMapper;
import com.hellfire.service.AddressService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private AddressService addressService;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> findUserByJwtToken(@RequestHeader("Authorization") String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return  new ResponseEntity<>(UserMapper.toDto(user), HttpStatus.OK);
    }


}
