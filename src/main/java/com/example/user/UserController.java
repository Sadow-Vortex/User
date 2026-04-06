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
    @Autowired
    private OtpService otpService;

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

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "File is empty")
            );
        }

        try {

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path uploadPath = Paths.get("uploads");

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(Map.of("filename", fileName));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed"));
        }
    }


    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<String>> sendOtp(@RequestBody SendOtpRequest request) {
        String email = request.getEmail();

        if (email == null || email.isBlank())
            return badReq("Email is required");

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            return badReq("Please provide a valid email address");

        // Check if already registered
        if (userRepository.findByEmail(email.trim().toLowerCase()).isPresent())
            return badReq("This email is already registered. Please login instead.");

        try {
            otpService.sendOtp(email.trim().toLowerCase());
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "OTP sent to " + email, "OTP_SENT")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Failed to send OTP: " + e.getMessage(), null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> registerWithOtp(@RequestBody RegisterWithOtpRequest req) {

        if (req.getEmail() == null || req.getOtp() == null)
            return badReq("Email and OTP are required");

        // Verify the OTP first
        OtpService.OtpVerifyResult result =
                otpService.verifyOtp(req.getEmail().trim().toLowerCase(), req.getOtp().trim());

        switch (result) {
            case NOT_FOUND -> { return badReq("No OTP was sent to this email. Please request a new one."); }
            case EXPIRED   -> { return badReq("OTP has expired. Please request a new OTP."); }
            case INVALID   -> { return badReq("Incorrect OTP. Please check and try again."); }
            case SUCCESS   -> { /* fall through */ }
        }

        // OTP verified — create user
        try {
            User user = new User(
                    req.getName().trim(),
                    req.getEmail().trim().toLowerCase(),
                    req.getNumber().trim(),
                    req.getPassword().trim()
            );
            User saved = userService.save(user);
            return ResponseEntity.ok(new ApiResponse<>(200, "Account created successfully", saved));
        } catch (Exception e) {
            e.printStackTrace();
            return badReq("Email or phone number already exists");
        }
    }

    @PostMapping("/")
    public ResponseEntity<ApiResponse<User>> addUser(@RequestBody User user) {
        try {
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "Success", userService.save(user)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "Email or number already exists", null));
        }
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(int code, String msg, T data) {
        return ResponseEntity.ok(new ApiResponse<>(code, msg, data));
    }

    private <T> ResponseEntity<ApiResponse<T>> badReq(String msg) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(400, msg, null));
    }
}

