package com.petpick.petpick.service;

import com.petpick.petpick.entity.UserEntity;

public interface userService1 {


        UserEntity findByAccountemail(String accountemail);
        boolean updateUserByEmail(String email, UserEntity formUser);
        // 其他方法...


}
