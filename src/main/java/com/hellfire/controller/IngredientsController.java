package com.hellfire.controller;

import com.hellfire.model.IngredientsCategory;
import com.hellfire.model.IngredientsItem;
import com.hellfire.model.User;
import com.hellfire.request.IngredientItemRequest;
import com.hellfire.request.IngredientsCategoryRequest;
import com.hellfire.service.IngredientsService;
import com.hellfire.service.RestaurantService;
import com.hellfire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ingredients")
@RequiredArgsConstructor
public class IngredientsController {

    private final IngredientsService ingredientsService;
    private final UserService userService;
    private final RestaurantService restaurantService;

    @PostMapping("/category")
    public ResponseEntity<IngredientsCategory> createIngredientsCategory(
            @Valid @RequestBody IngredientsCategoryRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        restaurantService.getRestaurantForUser(request.getRestaurantId(), user);

        IngredientsCategory category =
                ingredientsService.createIngredientsCategory(request.getName(), request.getRestaurantId());
        return new ResponseEntity<>(category, HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<IngredientsItem> createIngredientsItem(
            @Valid @RequestBody IngredientItemRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        restaurantService.getRestaurantForUser(request.getRestaurantId(), user);

        IngredientsItem item = ingredientsService.createIngredientsItem(
                request.getName(), request.getRestaurantId(), request.getCategoryId());
        return new ResponseEntity<>(item, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<IngredientsItem> updateIngredientStock(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) throws Exception {
        User user = userService.findUserByJwtToken(token);
        IngredientsItem item = ingredientsService.updateStock(id);
        return new ResponseEntity<>(item, HttpStatus.OK);
    }

    @GetMapping("/restaurant/{id}")
    public ResponseEntity<List<IngredientsItem>> getRestaurantIngredients(@PathVariable Long id) throws Exception {
        List<IngredientsItem> items = ingredientsService.findRestaurantIngredients(id);
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @GetMapping("/restaurant/{id}/category")
    public ResponseEntity<List<IngredientsCategory>> getRestaurantIngredientCategory(@PathVariable Long id) throws Exception {
        List<IngredientsCategory> categories = ingredientsService.findIngredientCategoryByRestaurantId(id);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }
}
