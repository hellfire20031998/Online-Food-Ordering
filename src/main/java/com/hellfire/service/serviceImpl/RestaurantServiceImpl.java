package com.hellfire.service.serviceImpl;

import com.hellfire.dto.RestaurantDto;
import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.exceptions.RestaurantException;
import com.hellfire.model.Address;
import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.model.UserRole;
import com.hellfire.repository.AddressRepository;
import com.hellfire.repository.RestaurantRepository;
import com.hellfire.repository.RestaurantRoleRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.request.CreateRestaurantRequest;
import com.hellfire.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final RestaurantRoleRepository restaurantRoleRepository;

    @Override
    @Transactional
    public Restaurant createRestaurant(CreateRestaurantRequest req, User user) {
        if (restaurantRepository.findByOwnerId(user.getId()) != null) {
            throw new IllegalArgumentException("You already have a restaurant");
        }

        Address address = addressRepository.save(req.getAddress());
        address.setUser(user);

        Restaurant restaurant = new Restaurant();
        restaurant.setAddress(address);
        restaurant.setContactInformation(req.getContactInformation());
        restaurant.setDescription(req.getDescription());
        restaurant.setName(req.getName());
        restaurant.setImages(req.getImages());
        restaurant.setOpeningHours(req.getOpeningHours());
        restaurant.setRegistrationDate(LocalDateTime.now());
        restaurant.setOwner(user);
        restaurant.setOpen(true);
        restaurant.setCuisineType(req.getCuisineType());
        return restaurantRepository.save(restaurant);
    }

    @Override
    @Transactional
    public Restaurant updateRestaurant(Long id, CreateRestaurantRequest updateRequest) throws Exception {
        Restaurant restaurant = findRestaurantById(id);

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
        if (updateRequest.getCuisineType() != null) {
            restaurant.setCuisineType(updateRequest.getCuisineType());
        }
        if (updateRequest.getImages() != null) {
            restaurant.setImages(updateRequest.getImages());
        }
        if (updateRequest.getOpeningHours() != null) {
            restaurant.setOpeningHours(updateRequest.getOpeningHours());
        }

        return restaurantRepository.save(restaurant);
    }

    @Override
    @Transactional
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
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantException("Restaurant with ID " + id + " not found"));
    }

    @Override
    public Restaurant getRestaurantByUserId(Long id) throws Exception {
        Restaurant restaurant = restaurantRepository.findByOwnerId(id);
        if (restaurant == null) {
            throw new RestaurantException("No restaurant found for this user");
        }
        return restaurant;
    }

    @Override
    public Restaurant getRestaurantForUser(Long restaurantId, User user) throws Exception {
        Restaurant restaurant = findRestaurantById(restaurantId);

        boolean isOwner = restaurant.getOwner() != null
                && Objects.equals(restaurant.getOwner().getId(), user.getId());
        boolean hasStaffRole = restaurantRoleRepository.findByUserAndRestaurant(user, restaurant).isPresent();

        if (!isOwner && !hasStaffRole) {
            throw new NotAuthorizedException("You are not allowed to manage this restaurant");
        }
        return restaurant;
    }

    @Override
    @Transactional
    public RestaurantDto addToFavourites(Long id, User user) throws Exception {
        Restaurant restaurant = findRestaurantById(id);

        RestaurantDto restaurantDto = new RestaurantDto();
        restaurantDto.setId(restaurant.getId());
        restaurantDto.setImages(
                restaurant.getImages() == null || restaurant.getImages().isEmpty()
                        ? null
                        : restaurant.getImages().get(0));
        restaurantDto.setDescription(restaurant.getDescription());
        restaurantDto.setTitle(restaurant.getName());

        boolean alreadyFavorite = user.getFavorites().stream()
                .anyMatch(fav -> Objects.equals(fav.getId(), restaurant.getId()));
        if (alreadyFavorite) {
            user.getFavorites().removeIf(fav -> Objects.equals(fav.getId(), restaurant.getId()));
        } else {
            user.getFavorites().add(restaurantDto);
        }
        userRepository.save(user);

        return restaurantDto;
    }

    @Override
    @Transactional
    public Restaurant updateRestaurantStatus(Long id) throws Exception {
        Restaurant restaurant = findRestaurantById(id);
        restaurant.setOpen(!restaurant.isOpen());
        return restaurantRepository.save(restaurant);
    }
}
