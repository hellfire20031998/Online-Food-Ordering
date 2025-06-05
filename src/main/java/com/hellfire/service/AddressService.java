package com.hellfire.service;

import com.hellfire.model.Address;

import java.util.List;

public interface AddressService {

    public Address addAddress(String token,Address address) throws Exception;
    public List<Address> getAddresses(String token) throws Exception;
    public void deleteAddressById(String token,Long addressId) throws Exception;
}
