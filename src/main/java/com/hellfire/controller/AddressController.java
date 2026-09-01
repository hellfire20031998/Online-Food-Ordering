package com.hellfire.controller;

import com.hellfire.model.Address;
import com.hellfire.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/getAddresses")
    public ResponseEntity<List<Address>> getUserAddresses(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        List<Address> addresses = addressService.getAddresses(token);
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    @PostMapping("/add_address")
    public ResponseEntity<Address> addAddress(@RequestBody Address address,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        Address saved = addressService.addAddress(token, address);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @DeleteMapping("/deleteAddress/{id}")
    public ResponseEntity<Void> deleteAddress(@RequestHeader(HttpHeaders.AUTHORIZATION) String token,
                                              @PathVariable Long id) throws Exception {
        addressService.deleteAddressById(token, id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
