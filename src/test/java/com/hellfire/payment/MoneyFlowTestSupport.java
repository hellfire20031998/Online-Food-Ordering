package com.hellfire.payment;

import com.hellfire.config.JwtProvider;
import com.hellfire.model.*;
import com.hellfire.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared fixtures for the payment / refund / payout flow tests. */
abstract class MoneyFlowTestSupport {

    @Autowired protected JwtProvider jwtProvider;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RestaurantRepository restaurantRepository;
    @Autowired protected RestaurantBankAccountRepository bankAccountRepository;
    @Autowired protected FoodRepository foodRepository;
    @Autowired protected CartRepository cartRepository;

    protected User user(String email, UserRole role) {
        User u = new User();
        u.setFullName(email.substring(0, email.indexOf('@')));
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("secret123"));
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setCreatedAt(LocalDateTime.now());
        return userRepository.save(u);
    }

    protected Restaurant restaurant(User owner, boolean withBankAccount) {
        Restaurant r = new Restaurant();
        r.setName("Test Kitchen");
        r.setCuisineType("Indian");
        r.setOwner(owner);
        r.setOpen(true);
        r.setStatus(RestaurantStatus.ACTIVE);
        r.setRegistrationDate(LocalDateTime.now());
        r = restaurantRepository.save(r);
        if (withBankAccount) {
            RestaurantBankAccount bank = new RestaurantBankAccount();
            bank.setRestaurant(r);
            bank.setAccountHolderName("Test Kitchen Pvt Ltd");
            bank.setAccountNumber("123456789012");
            bank.setIfsc("HDFC0001234");
            bank.setBankName("HDFC Bank");
            bank.setUpdatedAt(LocalDateTime.now());
            bank.setUpdatedBy("test");
            bankAccountRepository.save(bank);
        }
        return r;
    }

    protected Food food(Restaurant restaurant, String price) {
        Food f = new Food();
        f.setName("Paneer Tikka");
        f.setPrice(new BigDecimal(price));
        f.setRestaurant(restaurant);
        f.setAvailable(true);
        return foodRepository.save(f);
    }

    /** Puts {@code qty} of {@code food} in the customer's cart (creating the cart if needed). */
    protected void fillCart(User customer, Food food, int qty) {
        Cart cart = cartRepository.findByCustomerId(customer.getId());
        if (cart == null) {
            cart = new Cart();
            cart.setCustomer(customer);
            cart = cartRepository.save(cart);
        }
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setFood(food);
        item.setQuantity(qty);
        item.setIngredients(new ArrayList<>());
        item.setTotalPrice(food.getPrice().multiply(BigDecimal.valueOf(qty)));
        cart.getItems().add(item);
        cart.setTotal(item.getTotalPrice());
        cartRepository.save(cart);
    }

    protected int cartSize(User customer) {
        Cart cart = cartRepository.findByCustomerId(customer.getId());
        return cart == null ? 0 : cart.getItems().size();
    }

    protected static Map<String, Object> orderBody(Long restaurantId, String paymentMethod) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("restaurantId", restaurantId);
        body.put("paymentMethod", paymentMethod);
        body.put("deliveryAddress", Map.of("streetAddress", "1 MG Road", "city", "Pune", "state", "MH",
                "pincode", "411001", "country", "India"));
        return body;
    }

    protected String bearer(User u) {
        return "Bearer " + jwtProvider.generateToken(new UsernamePasswordAuthenticationToken(
                u.getEmail(), null, AuthorityUtils.createAuthorityList(u.getRole().name())));
    }
}
