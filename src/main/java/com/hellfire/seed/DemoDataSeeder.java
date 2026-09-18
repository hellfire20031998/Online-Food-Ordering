package com.hellfire.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellfire.model.*;
import com.hellfire.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds demo restaurants (owners, payout accounts, categories, ingredients, dishes) from
 * {@code seed/demo-restaurants.json} when {@code app.seed.demo} is true. Idempotent: a restaurant
 * whose name already exists is skipped, so the app can start any number of times.
 * Owner logins are {@code <slug>@<domain>} with the shared demo password.
 */
@Component
@Order(10)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    static final String SEED_FILE = "seed/demo-restaurants.json";

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantBankAccountRepository bankAccountRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientCategoryRepository ingredientCategoryRepository;
    private final IngredientItemRepository ingredientItemRepository;
    private final FoodRepository foodRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final boolean enabled;
    private final String demoPassword;

    public DemoDataSeeder(UserRepository userRepository,
                          CartRepository cartRepository,
                          RestaurantRepository restaurantRepository,
                          RestaurantBankAccountRepository bankAccountRepository,
                          CategoryRepository categoryRepository,
                          IngredientCategoryRepository ingredientCategoryRepository,
                          IngredientItemRepository ingredientItemRepository,
                          FoodRepository foodRepository,
                          PasswordEncoder passwordEncoder,
                          ObjectMapper objectMapper,
                          PlatformTransactionManager transactionManager,
                          @Value("${app.seed.demo:false}") boolean enabled,
                          @Value("${app.seed.demo-password:123}") String demoPassword) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.restaurantRepository = restaurantRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
        this.ingredientItemRepository = ingredientItemRepository;
        this.foodRepository = foodRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            log.info("Demo data seeding disabled (app.seed.demo=false)");
            return;
        }
        int created = seedAll();
        log.info("Demo data seeding finished: {} restaurant(s) created", created);
    }

    /**
     * Seeds every restaurant in the file that does not exist yet. Each restaurant is written in its
     * own transaction (explicitly, since a self-invoked @Transactional would be bypassed), so a
     * failure leaves no half-seeded restaurant behind. Returns how many were created.
     */
    public int seedAll() throws Exception {
        SeedFile file;
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            file = objectMapper.readValue(in, SeedFile.class);
        }
        int created = 0;
        for (SeedRestaurant seed : file.restaurants()) {
            Boolean didCreate = transactionTemplate.execute(status -> {
                if (restaurantRepository.existsByNameIgnoreCase(seed.name())) {
                    return false;
                }
                seedRestaurant(file, seed);
                return true;
            });
            if (Boolean.TRUE.equals(didCreate)) {
                created++;
            }
        }
        return created;
    }

    private void seedRestaurant(SeedFile file, SeedRestaurant seed) {
        User owner = ensureOwner(seed.owner(), file.ownerEmailDomain());

        Address address = new Address();
        address.setStreetAddress(seed.address().streetAddress());
        address.setCity(seed.address().city());
        address.setState(seed.address().state());
        address.setPincode(seed.address().pincode());
        address.setCountry(seed.address().country());
        address.setUser(owner);

        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(owner);
        restaurant.setName(seed.name());
        restaurant.setDescription(seed.description());
        restaurant.setCuisineType(seed.cuisineType());
        restaurant.setOpeningHours(seed.openingHours());
        restaurant.setAddress(address);
        restaurant.setContactInformation(new ContactInformation(
                seed.contact().email(), seed.contact().mobile(), blankToNull(seed.contact().twitter()), blankToNull(seed.contact().instagram())));
        restaurant.setImages(resolveImages(file.imageBase(), seed.images()));
        restaurant.setRegistrationDate(LocalDateTime.now());
        restaurant.setOpen(true);
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant = restaurantRepository.save(restaurant);

        if (seed.bankAccount() != null) {
            RestaurantBankAccount bank = new RestaurantBankAccount();
            bank.setRestaurant(restaurant);
            bank.setAccountHolderName(seed.bankAccount().accountHolderName());
            bank.setAccountNumber(seed.bankAccount().accountNumber());
            bank.setIfsc(seed.bankAccount().ifsc());
            bank.setBankName(seed.bankAccount().bankName());
            bank.setUpiId(blankToNull(seed.bankAccount().upiId()));
            bank.setUpdatedAt(LocalDateTime.now());
            bank.setUpdatedBy("seed");
            bankAccountRepository.save(bank);
        }

        Map<String, Category> categories = new HashMap<>();
        for (String name : seed.categories()) {
            Category category = new Category();
            category.setName(name);
            category.setRestaurant(restaurant);
            categories.put(name, categoryRepository.save(category));
        }

        Map<String, IngredientsItem> ingredients = new HashMap<>();
        for (SeedIngredientGroup group : seed.ingredientGroups()) {
            IngredientsCategory ingredientCategory = new IngredientsCategory();
            ingredientCategory.setName(group.name());
            ingredientCategory.setRestaurant(restaurant);
            ingredientCategory = ingredientCategoryRepository.save(ingredientCategory);
            for (String itemName : group.items()) {
                IngredientsItem item = new IngredientsItem();
                item.setName(itemName);
                item.setCategory(ingredientCategory);
                item.setRestaurant(restaurant);
                item.setInStock(true);
                ingredients.put(itemName, ingredientItemRepository.save(item));
            }
        }

        for (SeedFood seedFood : seed.foods()) {
            Food food = new Food();
            food.setName(seedFood.name());
            food.setDescription(seedFood.description());
            food.setPrice(BigDecimal.valueOf(seedFood.price()).setScale(2));
            food.setFoodCategory(categories.get(seedFood.category()));
            food.setRestaurant(restaurant);
            food.setAvailable(true);
            food.setVegetarian(seedFood.vegetarian());
            food.setSeasonal(seedFood.seasonal());
            food.setCreationDate(new Date());
            food.setImages(seedFood.image() == null ? new ArrayList<>()
                    : resolveImages(file.imageBase(), List.of(seedFood.image())));
            List<IngredientsItem> foodIngredients = new ArrayList<>();
            for (String ingredientName : seedFood.ingredients() == null ? List.<String>of() : seedFood.ingredients()) {
                IngredientsItem item = ingredients.get(ingredientName);
                if (item != null) {
                    foodIngredients.add(item);
                }
            }
            food.setIngredientsItems(foodIngredients);
            foodRepository.save(food);
        }

        log.info("Seeded restaurant '{}' ({} dishes) owned by {}", seed.name(), seed.foods().size(), owner.getEmail());
    }

    private User ensureOwner(SeedOwner seedOwner, String domain) {
        String email = seedOwner.email().contains("@") ? seedOwner.email() : seedOwner.email() + "@" + domain;
        User existing = userRepository.findByEmail(email);
        if (existing != null) {
            return existing;
        }
        User owner = new User();
        owner.setFullName(seedOwner.fullName());
        owner.setEmail(email);
        owner.setPassword(passwordEncoder.encode(demoPassword));
        owner.setRole(UserRole.ADMIN);
        owner.setStatus(UserStatus.ACTIVE);
        owner.setCreatedAt(LocalDateTime.now());
        owner = userRepository.save(owner);

        Cart cart = new Cart();
        cart.setCustomer(owner);
        cart.setTotal(BigDecimal.ZERO);
        cartRepository.save(cart);
        return owner;
    }

    private static List<String> resolveImages(String base, List<String> images) {
        List<String> urls = new ArrayList<>();
        for (String image : images == null ? List.<String>of() : images) {
            urls.add(image.contains("://") ? image : base + image);
        }
        return urls;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    // ------------------------------------------------------------------ file model

    record SeedFile(String imageBase, String ownerEmailDomain, List<SeedRestaurant> restaurants) {
    }

    record SeedRestaurant(String name, String description, String cuisineType, String openingHours,
                          SeedAddress address, SeedContact contact, List<String> images, SeedOwner owner,
                          SeedBankAccount bankAccount, List<String> categories,
                          List<SeedIngredientGroup> ingredientGroups, List<SeedFood> foods) {
    }

    record SeedAddress(String streetAddress, String city, String state, String pincode, String country) {
    }

    record SeedContact(String email, String mobile, String instagram, String twitter) {
    }

    record SeedOwner(String fullName, String email) {
    }

    record SeedBankAccount(String accountHolderName, String accountNumber, String ifsc, String bankName, String upiId) {
    }

    record SeedIngredientGroup(String name, List<String> items) {
    }

    record SeedFood(String name, String description, double price, String category, boolean vegetarian,
                    boolean seasonal, String image, List<String> ingredients) {
    }
}
