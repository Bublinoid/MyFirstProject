package com.example.manicurebot;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserStatusService {

    private final Map<Long, UserStatus> userStatusMap = new HashMap<>();
    private final Map<Long, String> userCommandMap = new HashMap<>();

    public void setUserStatus(long chatId, UserStatus userStatus) {
        userStatusMap.put(chatId, userStatus);
    }

    public UserStatus getUserStatus(long chatId) {
        return userStatusMap.getOrDefault(chatId, UserStatus.DEFAULT);
    }

    public void setUserCommand(long chatId, String command) {
        userCommandMap.put(chatId, command);
    }

    public String getUserCommand(long chatId) {
        return userCommandMap.get(chatId);
    }
}
