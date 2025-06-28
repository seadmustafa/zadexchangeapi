package com.zad.exchangeapi.service.impl;


import com.zad.exchangeapi.entity.User;
import com.zad.exchangeapi.repository.UserRepository;
import com.zad.exchangeapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new com.example.balanceapi.exception.NotFoundExceptionn("User not found with ID: " + userId));
    }
}
