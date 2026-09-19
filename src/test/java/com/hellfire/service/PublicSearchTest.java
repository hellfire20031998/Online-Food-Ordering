package com.hellfire.service;

import com.hellfire.model.*;
import com.hellfire.payment.MoneyFlowTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The public search endpoints behind the site's search box. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicSearchTest extends MoneyFlowTestSupport {

    @Autowired private MockMvc mockMvc;

    private Restaurant active;

    @BeforeEach
    void setUp() {
        active = restaurant(user("search.owner@test.com", UserRole.ADMIN), false);
        active.setName("Spice Route Kitchen");
        active.setCuisineType("North Indian");
        restaurantRepository.save(active);

        Restaurant suspended = restaurant(user("suspended.owner@test.com", UserRole.ADMIN), false);
        suspended.setName("Hidden Spice House");
        suspended.setStatus(RestaurantStatus.SUSPENDED);
        restaurantRepository.save(suspended);

        dish(active, "Paneer Tikka", "Smoky cottage cheese from the tandoor", true);
        dish(active, "Dal Makhani", "Slow-cooked black lentils", true);
        dish(active, "Paneer Butter Masala", "Sold out today", false);
        dish(suspended, "Paneer Bhurji", "Scrambled cottage cheese", true);
    }

    private void dish(Restaurant r, String name, String description, boolean available) {
        Food f = new Food();
        f.setName(name);
        f.setDescription(description);
        f.setPrice(new BigDecimal("250.00"));
        f.setRestaurant(r);
        f.setAvailable(available);
        foodRepository.save(f);
    }

    @Test
    void dishSearchIsCaseInsensitiveAndSkipsUnavailableOrSuspended() throws Exception {
        mockMvc.perform(get("/api/food/search").param("name", "PANEER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Paneer Tikka"))
                .andExpect(jsonPath("$[0].restaurant.name").value("Spice Route Kitchen"));
    }

    @Test
    void dishSearchMatchesDescriptions() throws Exception {
        mockMvc.perform(get("/api/food/search").param("name", "lentils"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Dal Makhani"));
    }

    @Test
    void restaurantSearchMatchesNameOrCuisineAndHidesSuspended() throws Exception {
        mockMvc.perform(get("/api/restaurants/search").param("name", "spice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Spice Route Kitchen"));

        mockMvc.perform(get("/api/restaurants/search").param("name", "north indian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void blankQueriesReturnNothingWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/food/search").param("name", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/restaurants/search").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
