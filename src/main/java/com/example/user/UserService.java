package com.example.user;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    public List<User> getAll(){
        return userRepository.findAll();
    }
    public Optional<User> getByEmail(String email){
        return userRepository.findByEmail(email);
    }
    public User save(User user){
        return userRepository.save(user);
    }
    public Optional<User> deleteById(Long id){
        userRepository.deleteById(id);
        return null;
    }
    public Optional<User> updateUser(long id,User user){
        Optional<User> optionalUser = userRepository.findById(id);
        if(optionalUser.isPresent()){
            optionalUser.get().setEmail(user.getEmail());
            optionalUser.get().setName(user.getName());
            optionalUser.get().setPassword(user.getPassword());
            optionalUser.get().setNumber(user.getNumber());
            userRepository.save(optionalUser.get());
        }
        return optionalUser;
    }

    public Optional<User> login(String number, String password) {

        return userRepository.findByNumber(number)
                .filter(user -> user.getPassword().equals(password));
    }
}

