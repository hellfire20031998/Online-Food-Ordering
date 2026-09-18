package com.hellfire.onboarding.controller;

import com.hellfire.model.User;
import com.hellfire.onboarding.dto.RestaurantApplicationDto;
import com.hellfire.onboarding.dto.RestaurantApplicationRequest;
import com.hellfire.onboarding.service.RestaurantApplicationService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Applicant side of restaurant onboarding. Any signed-in non-team user may apply. */
@RestController
@RequestMapping("/api/restaurant-applications")
@RequiredArgsConstructor
public class RestaurantApplicationController {

    private final RestaurantApplicationService applicationService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<RestaurantApplicationDto> submit(@Valid @RequestBody RestaurantApplicationRequest request,
                                                           @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return new ResponseEntity<>(applicationService.submit(user, request), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<List<RestaurantApplicationDto>> mine(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(applicationService.myApplications(user));
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<RestaurantApplicationDto> withdraw(@PathVariable Long id,
                                                             @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return ResponseEntity.ok(applicationService.withdraw(id, user));
    }
}
