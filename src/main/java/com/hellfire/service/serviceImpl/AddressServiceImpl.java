package com.hellfire.service.serviceImpl;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.model.Address;
import com.hellfire.model.User;
import com.hellfire.repository.AddressRepository;
import com.hellfire.service.AddressService;
import com.hellfire.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserService userService;

    @Override
    @Transactional
    public Address addAddress(String token, Address address) throws Exception {
        User user = userService.findUserByJwtToken(token);
        user.getAddresses().add(address);
        address.setUser(user);
        return addressRepository.save(address);
    }

    @Override
    public List<Address> getAddresses(String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        return user.getAddresses();
    }

    @Override
    @Transactional
    public void deleteAddressById(String token, Long addressId) throws Exception {
        User user = userService.findUserByJwtToken(token);
        Address addressToDelete = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        if (addressToDelete.getUser() == null
                || !addressToDelete.getUser().getId().equals(user.getId())) {
            throw new NotAuthorizedException("This address does not belong to you");
        }

        user.getAddresses().remove(addressToDelete);
        addressRepository.delete(addressToDelete);
    }
}
