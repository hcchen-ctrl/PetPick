package com.petpick.petpick.service;

import com.petpick.petpick.DTO.RegisterRequest;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements userService1{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
//處理註冊寫進資料庫
    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean registerNewUser(RegisterRequest request) {
        // 檢查是否已有同 email 使用者
        if (userRepository.findByAccountemail(request.getAccountemail()) != null) {
            return false;
        }

        // 確認密碼和確認密碼是否相同
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return false;
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setAccountemail(request.getAccountemail());
        user.setPhonenumber(request.getPhonenumber());

        // 這行非常重要，將密碼加密後再存入資料庫
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole("MANAGER");
        user.setIsaccount("1");
        user.setIsblacklist("0");

        userRepository.save(user);
        return true;
    }


    //更新個人資料進資料庫
    @Override
    public UserEntity findByAccountemail(String accountemail) {
        return userRepository.findByAccountemail(accountemail);
    }

    @Override
    public boolean updateUserByEmail(String email, UserEntity formUser) {
        UserEntity user = userRepository.findByAccountemail(email);
        if (user == null) return false;

        // 複製一般欄位，排除主鍵與敏感欄位
        BeanUtils.copyProperties(formUser, user, "userid", "accountemail", "password", "role", "isaccount", "isblacklist");

        // 多選欄位用 List 設定，確保字串欄位自動同步
        user.setPetList(formUser.getPetList());
        user.setPetActivitiesList(formUser.getPetActivitiesList());

        userRepository.save(user);
        return true;
    }



}
