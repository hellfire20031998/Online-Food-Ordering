package com.hellfire.seed;

import com.hellfire.model.*;
import com.hellfire.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boots with demo seeding on and checks the twelve restaurants land complete and idempotently.
 * Uses its own in-memory database so the seeded rows never leak into the other test contexts,
 * which share the default one.
 */
@SpringBootTest(properties = {
        "app.seed.demo=true",
        "app.seed.demo-password=123",
        "spring.datasource.url=jdbc:h2:mem:seeddb;MODE=MySQL;DB_CLOSE_DELAY=-1"})
@Transactional // lazy collections (images, ingredients) are read inside the test
class DemoDataSeederTest {

    @Autowired private DemoDataSeeder seeder;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private RestaurantBankAccountRepository bankAccountRepository;
    @Autowired private FoodRepository foodRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private IngredientItemRepository ingredientItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void seedsTwelveCompleteRestaurantsOnce() throws Exception {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(12, restaurants.size(), "startup seeding should have created every demo restaurant");

        for (Restaurant r : restaurants) {
            assertEquals(RestaurantStatus.ACTIVE, r.getStatus(), r.getName());
            assertTrue(r.isOpen(), r.getName());
            assertNotNull(r.getAddress(), r.getName());
            assertNotNull(r.getContactInformation(), r.getName());
            assertTrue(r.getImages().size() >= 3, r.getName() + " needs at least 3 images");
            r.getImages().forEach(url -> assertTrue(url.startsWith("https://res.cloudinary.com/"), url));

            User owner = r.getOwner();
            assertEquals(UserRole.ADMIN, owner.getRole(), r.getName());
            assertTrue(owner.getEmail().endsWith("@foodiyapa.com"), owner.getEmail());
            assertTrue(passwordEncoder.matches("123", owner.getPassword()), "demo password for " + owner.getEmail());
            assertNotNull(cartRepository.findByCustomerId(owner.getId()), "owner needs a cart");

            assertTrue(bankAccountRepository.findByRestaurantId(r.getId()).isPresent(), r.getName() + " needs a payout account");
            assertTrue(categoryRepository.findByRestaurantId(r.getId()).size() >= 3, r.getName() + " needs categories");
            assertTrue(ingredientItemRepository.findByRestaurantId(r.getId()).size() >= 4, r.getName() + " needs ingredients");

            List<Food> foods = foodRepository.findByRestaurantId(r.getId());
            assertTrue(foods.size() >= 6, r.getName() + " needs at least 6 dishes");
            foods.forEach(f -> {
                assertNotNull(f.getFoodCategory(), f.getName() + " needs a category");
                assertTrue(f.getPrice().signum() > 0, f.getName() + " needs a price");
                assertTrue(f.isAvailable(), f.getName());
            });
        }

        Restaurant spiceRoute = restaurants.stream().filter(r -> r.getName().equals("Spice Route Kitchen")).findFirst().orElseThrow();
        assertEquals("spiceroutekitchen@foodiyapa.com", spiceRoute.getOwner().getEmail());
        assertEquals("Delhi", spiceRoute.getAddress().getCity());
        assertEquals("123456789012".length(), bankAccountRepository.findByRestaurantId(spiceRoute.getId()).orElseThrow().getAccountNumber().length());

        // Running again changes nothing.
        long users = userRepository.count();
        long foods = foodRepository.count();
        assertEquals(0, seeder.seedAll());
        assertEquals(12, restaurantRepository.count());
        assertEquals(users, userRepository.count());
        assertEquals(foods, foodRepository.count());
    }
}
