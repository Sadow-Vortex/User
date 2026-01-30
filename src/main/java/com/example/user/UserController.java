package com.example.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> user = userService.getAll();
        if (user.isEmpty()) {
            ApiResponse<List<User>> response = new ApiResponse<>(
                    HttpStatus.NOT_FOUND.value(),
                    "Error",
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        ApiResponse<List<User>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Success",
                user
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<User>> addUser(@RequestBody User user) {
        User us = userService.save(user);
        ApiResponse<User> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Success",
                us
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable long id) {
        Optional<User> useropt = userRepository.findById(id);
        if (useropt.isEmpty()) {
            ApiResponse<User> response = new ApiResponse<>(
                    HttpStatus.NOT_FOUND.value(),
                    "Error",
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        User user = useropt.get();
        ApiResponse<User> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Success",
                user
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable long id, @RequestBody User user) {
        Optional<User> useropt = userRepository.findById(id);
        if (useropt.isEmpty()) {
            ApiResponse<User> response = new ApiResponse<>(
                    HttpStatus.NOT_FOUND.value(),
                    "Error",
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        if(useropt.isPresent()) {
            Optional<User> us = userService.updateUser(id,user);
            ApiResponse<User> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Success",
                    user
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return  new ResponseEntity<>(HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<User>> deleteUser(@PathVariable long id) {
        Optional<User> useropt = userRepository.findById(id);
        if (useropt.isEmpty()) {
            ApiResponse<User> response = new ApiResponse<>(
                    HttpStatus.NOT_FOUND.value(),
                    "Error",
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        if(useropt.isPresent()) {
            Optional<User> us = userService.deleteById(id);
            ApiResponse<User> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Sucsess",
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return  new ResponseEntity<>(HttpStatus.OK);
    }
    @GetMapping("/login")
    public ResponseEntity<ApiResponse<User>> getUserByNameAndPassword(@PathVariable String number, @PathVariable String password) {
        Optional<User> useropt = userRepository.findByNumber(number);
        if (useropt.isEmpty()) {
            ApiResponse<User> response = new ApiResponse<>(
                    HttpStatus.NOT_FOUND.value(),
                    "Error",
                    null
            );
        }
        Optional<User> us = userService.login(number, password);
        ApiResponse<User> response = new ApiResponse<User>(
                HttpStatus.OK.value(),
                "Success",
                null
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

