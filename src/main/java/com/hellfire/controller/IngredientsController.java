package com.hellfire.controller;


import com.hellfire.model.IngredientsCategory;
import com.hellfire.model.IngredientsItem;
import com.hellfire.request.IngredientItemRequest;
import com.hellfire.request.IngredientsCategoryRequest;
import com.hellfire.service.IngredientsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ingredients")
public class IngredientsController {

    @Autowired
    private IngredientsService ingredientsService;

    @PostMapping("/category")
    public ResponseEntity<IngredientsCategory> createIngredientsCategory(@RequestBody IngredientsCategoryRequest request
                                                                         ) throws Exception {

        IngredientsCategory ingredientsCategory= ingredientsService.createIngredientsCategory(request.getName(), request.getRestaurantId());

        System.out.println("Ingredient category created controller"+ request.getName());
        return new ResponseEntity<>(ingredientsCategory, HttpStatus.CREATED);

    }

    @PostMapping()
    public ResponseEntity<IngredientsItem> createIngredientsItem(@RequestBody IngredientItemRequest request
    ) throws Exception {

      IngredientsItem ingredientsItem = ingredientsService.createIngredientsItem(request.getName(), request.getRestaurantId(),request.getCategoryId());


      return new ResponseEntity<>(ingredientsItem, HttpStatus.CREATED);

    }

    @PutMapping("/{id}/stock")
    public  ResponseEntity<?> updateIngredientStock(@PathVariable Long id) throws Exception {
        IngredientsItem ingredientsItem = ingredientsService.updateStock(id);
        return  new ResponseEntity<>(ingredientsItem, HttpStatus.OK);
    }

    @GetMapping("/restaurant/{id}")
    public  ResponseEntity<?> getRestaurantIngredient(@PathVariable Long id) throws Exception {
        List<IngredientsItem> ingredientsItem = ingredientsService.findRestaurantIngredients(id);
        return  new ResponseEntity<>(ingredientsItem, HttpStatus.OK);
    }

    @GetMapping("/restaurant/{id}/category")
    public  ResponseEntity<?> getRestaurantIngredientCategory(@PathVariable Long id) throws Exception {
        List<IngredientsCategory> ingredientsItem = ingredientsService.findIngredientCategoryByRestaurantId(id);
        return  new ResponseEntity<>(ingredientsItem, HttpStatus.OK);
    }



}
