package com.petpick.petpick.service;

import com.petpick.petpick.entity.userEntity;

public interface userService {
    void register(userEntity user);

    boolean loginByEmail(String accountemail, String password); // 新增：用 email 登入
    boolean login(String accountemail, String password);

    userEntity findById(Long user_id);
    void save(userEntity user);

}
