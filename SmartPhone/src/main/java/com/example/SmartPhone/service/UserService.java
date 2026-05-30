package com.example.SmartPhone.service;

import com.example.SmartPhone.entity.User;
import com.example.SmartPhone.model.RegistrationRequest;

public interface UserService {
    User registerUser(RegistrationRequest request) throws Exception;
    User findByUsername(String username);
    void deleteUser(Long userId);
}
