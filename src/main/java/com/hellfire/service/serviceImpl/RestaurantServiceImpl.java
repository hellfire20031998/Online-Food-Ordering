package com.hellfire.service.serviceImpl;


import com.hellfire.Dto.RestaurantDto;
import com.hellfire.exceptions.RestaurantException;
import com.hellfire.model.Address;
import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.repository.AddressRepository;
import com.hellfire.repository.RestaurantRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.request.CreateRestaurantRequest;
import com.hellfire.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Restaurant createRestaurant(CreateRestaurantRequest req, User user) {

        Address address = addressRepository.save(req.getAddress());
        Restaurant restaurant = new Restaurant();
        address.setUser(user);
        restaurant.setAddress(address);
        restaurant.setContactInformation(req.getContactInformation());
        restaurant.setDescription(req.getDescription());
        restaurant.setName(req.getName());
        restaurant.setImages(req.getImages());
        restaurant.setOpeningHours(req.getOpeningHours());
        restaurant.setRegistrationDate(LocalDateTime.now());
        restaurant.setOwner(user);
        restaurant.setOpen(true);
//        System.out.println("restaurant created "+restaurant);
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public Restaurant updateRestaurant(Long id, CreateRestaurantRequest updateRequest) throws Exception {
        // Fetch existing restaurant (handle entity not found)
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new Exception("Restaurant with ID " + id + " not found"));

        // Update fields only if the new values are not null
        if (updateRequest.getAddress() != null) {
            restaurant.setAddress(updateRequest.getAddress());
        }
        if (updateRequest.getContactInformation() != null) {
            restaurant.setContactInformation(updateRequest.getContactInformation());
        }
        if (updateRequest.getDescription() != null) {
            restaurant.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getName() != null) {
            restaurant.setName(updateRequest.getName());
        }
        if (updateRequest.getImages() != null) {
            restaurant.setImages(updateRequest.getImages());
        }
        if (updateRequest.getOpeningHours() != null) {
            restaurant.setOpeningHours(updateRequest.getOpeningHours());
        }

        // Save and return updated restaurant
        return restaurantRepository.save(restaurant);
    }

    @Override
    public void deleteRestaurant(Long id) throws Exception {
        Restaurant restaurant = findRestaurantById(id);
        restaurantRepository.delete(restaurant);

    }

    @Override
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    @Override
    public List<Restaurant> searchRestaurant(String query) {
        return restaurantRepository.findBySearchQuery(query);
    }

    @Override
    public Restaurant findRestaurantById(Long id) throws Exception {
        Optional<Restaurant> restaurant = restaurantRepository.findById(id);

        if(restaurant.isPresent()) {
            return restaurant.get();
        }
        throw  new RestaurantException("Restaurant with ID " + id + " not found");
    }

    @Override
    public Restaurant getRestaurantByUserId(Long id) throws Exception {
        Restaurant restaurant = restaurantRepository.findByOwnerId(id);
        if(restaurant != null) {
            return restaurant;
        }
        throw  new RestaurantException("Restaurant with ID " + id + " not found");
    }

    @Override
    public RestaurantDto addToFavourites(Long id, User user) throws Exception {
        Restaurant restaurant = findRestaurantById(id);

        RestaurantDto restaurantDto = new RestaurantDto();
        restaurantDto.setId(restaurant.getId());
        restaurantDto.setImages(restaurant.getImages().get(0));
        restaurantDto.setDescription(restaurant.getDescription());
        restaurantDto.setTitle(restaurant.getName());


////        Ensure the user's favorites list is initialized
//        if (user.getFavorties() == null) {
//            user.setFavorties(new HashSet<>()); // Assuming Set<Restaurant>
//        }

        // Check if the restaurant is already in favorites
        if (user.getFavorites().stream().anyMatch(fav -> fav.getId().equals(restaurant.getId()))) {
            user.getFavorites().removeIf(fav -> fav.getId().equals(restaurant.getId())); // Remove from favorites
        } else {
            user.getFavorites().add(restaurantDto); // Add to favorites
        }
        userRepository.save(user);

        return restaurantDto;
    }

    @Override
    public Restaurant updateRestaurantStatus(Long id) throws Exception {
        Restaurant restaurant = findRestaurantById(id);
        restaurant.setOpen(!restaurant.isOpen());
        return restaurantRepository.save(restaurant);
    }


}
