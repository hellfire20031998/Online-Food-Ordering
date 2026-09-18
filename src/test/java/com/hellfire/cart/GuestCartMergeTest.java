package com.hellfire.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellfire.model.Food;
import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.model.UserRole;
import com.hellfire.payment.MoneyFlowTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Single-restaurant cart rule, the replace flag, and carrying a guest cart into an account. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GuestCartMergeTest extends MoneyFlowTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private User customer;
    private Food paneer;   // restaurant A
    private Food dosa;     // restaurant B
    private Food unavailable;

    @BeforeEach
    void setUp() {
        customer = user("cust@test.local", UserRole.CUSTOMER);
        Restaurant a = restaurant(user("ownerA@test.local", UserRole.ADMIN), false);
        Restaurant b = restaurant(user("ownerB@test.local", UserRole.ADMIN), false);
        b.setName("Dosa Corner");
        restaurantRepository.save(b);
        paneer = food(a, "100.00");
        dosa = food(b, "60.00");
        unavailable = food(a, "80.00");
        unavailable.setAvailable(false);
        foodRepository.save(unavailable);
        // Public signup normally creates the cart; do it here directly.
        fillCartShell(customer);
    }

    @Test
    void cartHoldsOneRestaurantUnlessReplaced() throws Exception {
        add(paneer.getId(), false).andExpect(status().isOk());

        add(dosa.getId(), false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Test Kitchen")));

        add(dosa.getId(), true).andExpect(status().isOk());

        mockMvc.perform(get("/api/cart/").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.restaurantName").value("Dosa Corner"))
                .andExpect(jsonPath("$.items[0].food.name").value("Paneer Tikka"))
                .andExpect(jsonPath("$.items[0].food.restaurantId").value(dosa.getRestaurant().getId()))
                .andExpect(jsonPath("$.items[0].totalPrice").value(60.0));
    }

    @Test
    void sameDishWithDifferentIngredientsIsASeparateLine() throws Exception {
        add(paneer.getId(), false, List.of("Onion")).andExpect(status().isOk());
        add(paneer.getId(), false, List.of("Onion")).andExpect(status().isOk());
        add(paneer.getId(), false, List.of("Cheese")).andExpect(status().isOk());

        mockMvc.perform(get("/api/cart/").header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.total").value(300.0));
    }

    @Test
    void mergeIntoEmptyCartSkipsWhatIsNotOrderable() throws Exception {
        merge("MERGE", List.of(
                guest(paneer.getId(), 2, List.of()),
                guest(unavailable.getId(), 1, List.of()),
                guest(99999L, 1, List.of()),
                guest(dosa.getId(), 1, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.items", hasSize(1)))
                .andExpect(jsonPath("$.cart.items[0].quantity").value(2))
                .andExpect(jsonPath("$.cart.total").value(200.0))
                .andExpect(jsonPath("$.skipped", hasSize(3)))
                .andExpect(jsonPath("$.skipped[0].reason").value("Currently unavailable"))
                .andExpect(jsonPath("$.skipped[1].reason").value("No longer on the menu"))
                .andExpect(jsonPath("$.skipped[2].reason").value("From a different restaurant than the rest of the cart"));
    }

    @Test
    void mergeStrategiesAgainstAnExistingCart() throws Exception {
        add(paneer.getId(), false).andExpect(status().isOk()); // account cart: 1 paneer from A

        // MERGE with the same restaurant adds up.
        merge("MERGE", List.of(guest(paneer.getId(), 1, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.items", hasSize(1)))
                .andExpect(jsonPath("$.cart.items[0].quantity").value(2));

        // MERGE with another restaurant is a conflict the client must resolve.
        merge("MERGE", List.of(guest(dosa.getId(), 1, List.of())))
                .andExpect(status().isConflict());

        // KEEP_SERVER discards the guest items.
        merge("KEEP_SERVER", List.of(guest(dosa.getId(), 1, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.items", hasSize(1)))
                .andExpect(jsonPath("$.cart.restaurantName").value("Test Kitchen"));

        // REPLACE switches the cart to the guest restaurant.
        merge("REPLACE", List.of(guest(dosa.getId(), 3, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.items", hasSize(1)))
                .andExpect(jsonPath("$.cart.restaurantName").value("Dosa Corner"))
                .andExpect(jsonPath("$.cart.total").value(180.0));
    }

    // ------------------------------------------------------------------ helpers

    private org.springframework.test.web.servlet.ResultActions add(Long foodId, boolean replace) throws Exception {
        return add(foodId, replace, List.of());
    }

    private org.springframework.test.web.servlet.ResultActions add(Long foodId, boolean replace, List<String> ingredients) throws Exception {
        return mockMvc.perform(post("/api/cart/add").header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "foodId", foodId, "quantity", 1, "ingredients", ingredients, "replaceCart", replace))));
    }

    private org.springframework.test.web.servlet.ResultActions merge(String strategy, List<Map<String, Object>> items) throws Exception {
        return mockMvc.perform(post("/api/cart/merge").header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("strategy", strategy, "items", items))));
    }

    private static Map<String, Object> guest(Long foodId, int qty, List<String> ingredients) {
        return Map.of("foodId", foodId, "quantity", qty, "ingredients", ingredients);
    }

    private void fillCartShell(User user) {
        com.hellfire.model.Cart cart = new com.hellfire.model.Cart();
        cart.setCustomer(user);
        cartRepository.save(cart);
    }
}
