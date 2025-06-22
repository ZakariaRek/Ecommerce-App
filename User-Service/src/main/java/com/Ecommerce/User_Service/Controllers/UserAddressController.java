
package com.Ecommerce.User_Service.Controllers;

import com.Ecommerce.User_Service.Models.UserAddress;
import com.Ecommerce.User_Service.Payload.Response.MessageResponse;
import com.Ecommerce.User_Service.Services.UserAddressService;
import com.Ecommerce.User_Service.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
//@CrossOrigin(origins = "*", maxAge = 3600)
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserAddress>> getAllAddresses() {
        List<UserAddress> addresses = userAddressService.getAllAddresses();
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddressById(@PathVariable String id) {
        return userAddressService.getAddressById(id)
                .map(address -> new ResponseEntity<>(address, HttpStatus.OK))
                .orElse(new ResponseEntity
                        (  new MessageResponse("Address not found"),HttpStatus.NOT_FOUND)
                      );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getAddressesByUserId(@PathVariable String userId) {
        if (!userService.getUserById(userId).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("User not found"));
        }
        List<UserAddress> addresses = userAddressService.getAddressesByUserId(userId);
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}/default")
    public ResponseEntity<?> getDefaultAddress(@PathVariable String userId) {
        if (!userService.getUserById(userId).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("User not found"));
        }
        return userAddressService.getDefaultAddress(userId)
                .map(address -> new ResponseEntity<>(address, HttpStatus.OK))
                .orElse(new ResponseEntity
                        (  new MessageResponse("Default address not found"),HttpStatus.NOT_FOUND)
                     );
    }

    @PostMapping
    public ResponseEntity<?> createAddress(@Valid @RequestBody UserAddress address) {
        if (!userService.getUserById(address.getUserId()).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("User not found"));
        }
        UserAddress savedAddress = userAddressService.createAddress(address);
        return new ResponseEntity<>(savedAddress, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(@PathVariable String id, @Valid @RequestBody UserAddress address) {
        return userAddressService.getAddressById(id)
                .map(existingAddress -> {
                    address.setId(id);
                    if (!userService.getUserById(address.getUserId()).isPresent()) {
                        return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(new MessageResponse("User not found"));
                    }
                    UserAddress updatedAddress = userAddressService.updateAddress(address);
                    return new ResponseEntity<>(updatedAddress, HttpStatus.OK);
                })
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Address not found")));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<?> setAddressAsDefault(@PathVariable String id) {
        return userAddressService.getAddressById(id)
                .map(existingAddress -> {
                    UserAddress updatedAddress = userAddressService.setAddressAsDefault(id);
                    return new ResponseEntity<>(updatedAddress, HttpStatus.OK);
                })
                .orElse(new ResponseEntity
                        (new MessageResponse("Address not found"),HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable String id) {
        return userAddressService.getAddressById(id)
                .map(address -> {
                    userAddressService.deleteAddress(id);
                    return ResponseEntity
                            .ok()
                            .body(new MessageResponse("Address deleted successfully"));
                })
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Address not found")));
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> deleteAddressesByUserId(@PathVariable String userId) {
        if (!userService.getUserById(userId).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("User not found"));
        }
        userAddressService.deleteAddressesByUserId(userId);
        return ResponseEntity
                .ok()
                .body(new MessageResponse("All addresses for user deleted successfully"));
    }
}