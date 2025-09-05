package com.petpick.petpick.service;

import com.petpick.petpick.entity.UserEntity;

import java.util.Optional;

public interface userService1 {


        UserEntity findByAccountemail(String accountemail);
        boolean updateUserByEmail(String email, UserEntity formUser);

    boolean updateUser(Long userId, UserEntity formUser);  // 👈 這個也加進去
    Optional<UserEntity> findById(Long userId);


    //處理使用者更改帳號密碼
    String changePassword(String email, String currentPassword, String newPassword, String confirmPassword);
    // 其他方法...


}
