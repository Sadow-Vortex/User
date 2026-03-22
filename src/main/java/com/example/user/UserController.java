package com.example.user;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Cloudinary cloudinary;

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

    @PostMapping("/")
    public ResponseEntity<ApiResponse<User>> addUser(@RequestBody User user) {
        System.out.println("API HIT");

        try {

            User us = userService.save(user);

            ApiResponse<User> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Success",
                    us
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();   // IMPORTANT for you now

            ApiResponse<User> response = new ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Email or number already exists",
                    null
            );

            return ResponseEntity.badRequest().body(response);
        }
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<User>> login(
            @RequestBody User user) {

        Optional<User> useropt =
                userService.login(user.getNumber(), user.getPassword());

        if (useropt.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            HttpStatus.UNAUTHORIZED.value(),
                            "Invalid number or password",
                            null
                    )
            );
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                        HttpStatus.OK.value(),
                        "Login success",
                        useropt.get()
                )
        );
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

            String imageUrl = uploadResult.get("secure_url").toString();

            return ResponseEntity.ok(Map.of("url", imageUrl));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed");
        }
    }
}

