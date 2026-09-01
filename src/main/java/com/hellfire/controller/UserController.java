package com.hellfire.controller;

import com.hellfire.model.User;
import com.hellfire.service.UserService;
import com.hellfire.user.dto.UserDto;
import com.hellfire.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> findUserByJwtToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return new ResponseEntity<>(UserMapper.toDto(user), HttpStatus.OK);
    }
}
