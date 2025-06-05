package com.hellfire.controller;


import com.hellfire.model.Restaurant;
import com.hellfire.model.User;
import com.hellfire.request.CreateRestaurantRequest;
import com.hellfire.response.MessageResponse;
import com.hellfire.service.RestaurantService;
import com.hellfire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/restaurants")
public class AdminRestaurantController {

    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private UserService userService;

    @PostMapping()
    public ResponseEntity<Restaurant>  createRestaurant(@RequestBody CreateRestaurantRequest req,
                                                        @RequestHeader("Authorization") String jwt) throws Exception {


       try{
           User user=userService.findUserByJwtToken(jwt);

           Restaurant restaurant=restaurantService.createRestaurant(req,user);
           System.out.println("Restaurant created ");
           return  new ResponseEntity<>(restaurant, HttpStatus.CREATED);

       }catch (Exception e){
           System.out.println("Restaurant creation failed"+e.getMessage());
           return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
       }

    }


    @PutMapping("/{id}")
    public ResponseEntity<Restaurant>  updateRestaurant(@RequestBody CreateRestaurantRequest req,
                                                        @RequestHeader("Authorization") String jwt,
                                                        @PathVariable Long id) throws Exception {

        User user=userService.findUserByJwtToken(jwt);

        Restaurant restaurant=restaurantService.updateRestaurant(id,req);
        return  new ResponseEntity<>(restaurant, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse>  deleteRestaurant(
                                                        @RequestHeader("Authorization") String jwt,
                                                        @PathVariable Long id) throws Exception {

        User user=userService.findUserByJwtToken(jwt);

        restaurantService.deleteRestaurant(id);
        MessageResponse response = new MessageResponse();
        response.setMessage("Restaurant deleted");
        return  new ResponseEntity<>(response,HttpStatus.OK);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Restaurant>  updateRestaurantStatus(
                                                        @RequestHeader("Authorization") String jwt,
                                                        @PathVariable Long id) throws Exception {

        User user=userService.findUserByJwtToken(jwt);

        Restaurant restaurant=restaurantService.updateRestaurantStatus(id);
        return  new ResponseEntity<>(restaurant, HttpStatus.OK);
    }

    @GetMapping("/user")
    public ResponseEntity<Restaurant>  findRestaurantByUserId(
                                                              @RequestHeader("Authorization") String jwt
                                                             ) throws Exception {
        User user=userService.findUserByJwtToken(jwt);

        Restaurant restaurant=  restaurantService.getRestaurantByUserId(user.getId());
        if(restaurant==null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return  new ResponseEntity<>(restaurant, HttpStatus.OK);
    }





}
