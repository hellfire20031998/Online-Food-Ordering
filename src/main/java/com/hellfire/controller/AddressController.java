package com.hellfire.controller;

import com.hellfire.model.Address;
import com.hellfire.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class AddressController {

    @Autowired
    AddressService addressService;

    @GetMapping("/getAddresses")
    public ResponseEntity<?> getUserAddresses(@RequestHeader("Authorization") String token) throws Exception {

        List<Address> addresses= addressService.getAddresses(token);
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }
    @PostMapping("/add_address")
    public ResponseEntity<?> addAddress(@RequestBody Address address,
                                       @RequestHeader("Authorization") String token)  {

        try {
            addressService.addAddress(token, address);
            return new ResponseEntity<>(address,HttpStatus.CREATED);
        }catch (Exception e){
            return new ResponseEntity<>("Invalid Address or user", HttpStatus.BAD_REQUEST);
        }
    }
    @DeleteMapping("/deleteAddress/{id}")
    public ResponseEntity<?> deleteAddress(@RequestHeader("Authorization") String token,
                                           @PathVariable Long id) {
        try {
            addressService.deleteAddressById(token, id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid Address or User", HttpStatus.BAD_REQUEST);
        }
    }

}
