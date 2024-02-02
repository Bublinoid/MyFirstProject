package com.example.manicurebot.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * logger
 */

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String firstName, String lastName, String username) {
        User newUser = new User(firstName, lastName, username);
        User savedUser = userRepository.save(newUser);
        logger.info("User saved: " + savedUser);
        return savedUser;
    }

    public User createUserIfNotExist(String firstName, String lastName, String username) {
        User existingUser = userRepository.findByUsername(username);

        if (existingUser == null) {
            // Пользователь не найден, создаем нового
            User newUser = new User(firstName, lastName, username);
            User savedUser = userRepository.save(newUser);
            logger.info("Создан новый пользователь: {}", savedUser);
            return savedUser;
        } else {
            // Пользователь уже существует, возвращаем существующего пользователя
            logger.info("Найден существующий пользователь: {}", existingUser);
            return existingUser;
        }
    }


    public User getUserByUsername(String username) {
        logger.info("Ищем пользователя с именем пользователя: {}", username);
        User user = userRepository.findByUsername(username);

        if (user == null) {
            logger.warn("Пользователь не найден для имени пользователя: {}", username);

            // Если пользователь не найден, создаем нового пользователя
            user = createUserIfNotExist("DefaultFirstName", "DefaultLastName", username);
        } else {
            logger.info("Пользователь найден: {}", user);
        }

        return user;
    }
}
