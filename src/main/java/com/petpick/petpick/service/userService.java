package com.petpick.petpick.service;

import com.petpick.petpick.entity.userEntity;

public interface userService {
    void register(userEntity user);

    boolean login(String username, String password);
}
