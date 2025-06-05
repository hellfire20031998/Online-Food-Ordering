package com.hellfire.controller;


import com.hellfire.model.Category;
import com.hellfire.model.User;
import com.hellfire.service.CategoryService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @PostMapping("/admin/category")
    public ResponseEntity<Category>  createCategory(@RequestBody Category category,
                                                    @RequestHeader("Authorization") String token) throws Exception {
        User user = userService.findUserByJwtToken(token);

        Category createdCategory =categoryService.createCategory(category.getName(),user.getId());

        return  new ResponseEntity<>(createdCategory, HttpStatus.CREATED);


    }
    @GetMapping("/category/restaurant/{id}")
    public ResponseEntity<List<Category>>  createRestaurantCategory(
            @PathVariable Long id,
                                                    @RequestHeader("Authorization") String token) throws Exception {
        User user = userService.findUserByJwtToken(token);

        List<Category> createdCategory =categoryService.findCategoryByRestaurantId(id);

        return  new ResponseEntity<>(createdCategory, HttpStatus.CREATED);


    }

}
