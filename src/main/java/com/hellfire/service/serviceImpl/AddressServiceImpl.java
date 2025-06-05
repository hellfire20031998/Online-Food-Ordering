package com.hellfire.service.serviceImpl;

import com.hellfire.model.Address;
import com.hellfire.model.User;
import com.hellfire.repository.AddressRepository;
import com.hellfire.service.AddressService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private UserService userService;

    @Override
    public Address addAddress(String token, Address address) throws Exception {
        User user = userService.findUserByJwtToken(token);
        if (user == null) {
            throw new Exception("User not found");
        }
        user.getAddresses().add(address);
        address.setUser(user);
//        System.out.println("Address created"+address);
        return addressRepository.save(address);
    }


    @Override
    public List<Address> getAddresses(String token)throws Exception {
        User user = userService.findUserByJwtToken(token);

        if (user == null) {
            throw new Exception();
        }
        return user.getAddresses();
    }
    public void deleteAddressById(String token, Long addressId) throws Exception {
        User user = userService.findUserByJwtToken(token);
        System.out.println("AddressId: " + addressId);
        Address addressToDelete = addressRepository.findById(addressId)
                .orElseThrow(() -> new Exception("Address not found"));

        System.out.println("AddressToDelete: " + addressToDelete);
//        if (!addressToDelete.getUser().getId().equals(user.getId())) {
//            System.out.println("user with address not found");
//            throw new Exception("Address does not belong to the user");
//        }else{
//            System.out.println("address not found");
//        }

        addressRepository.delete(addressToDelete);
    }


}
