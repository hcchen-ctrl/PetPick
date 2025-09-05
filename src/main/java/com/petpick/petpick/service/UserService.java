package com.petpick.petpick.service;

import com.petpick.petpick.DTO.RegisterRequest;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserService implements userService1 {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public Optional<UserEntity> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ 修正後的註冊方法
    public boolean registerNewUser(RegisterRequest request) {
        System.out.println("🔍 開始註冊流程，檢查使用者: " + request.getAccountemail());

        try {
            // ✅ 檢查輸入資料
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                System.out.println("❌ 姓名不能為空");
                return false;
            }

            if (request.getAccountemail() == null || request.getAccountemail().trim().isEmpty()) {
                System.out.println("❌ 信箱不能為空");
                return false;
            }

            if (request.getPassword() == null || request.getPassword().length() < 6) {
                System.out.println("❌ 密碼長度不足");
                return false;
            }

            // ✅ 檢查是否已有同 email 使用者（修正為 Optional 處理）
            Optional<UserEntity> existingUserOpt = userRepository.findByAccountemail(request.getAccountemail());
            if (existingUserOpt.isPresent()) {
                System.out.println("❌ 信箱已存在: " + request.getAccountemail());
                return false;
            }

            // ✅ 檢查用戶名是否已存在
            Optional<UserEntity> existingUsernameOpt = userRepository.findByUsername(request.getUsername());
            if (existingUsernameOpt.isPresent()) {
                System.out.println("❌ 用戶名已存在: " + request.getUsername());
                return false;
            }

            // ✅ 建立新用戶
            UserEntity user = new UserEntity();
            user.setUsername(request.getUsername().trim());
            user.setAccountemail(request.getAccountemail().trim().toLowerCase());
            user.setPhonenumber(request.getPhonenumber() != null ? request.getPhonenumber().trim() : null);

            // ✅ 加密密碼
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            // ✅ 設定預設值
            user.setRole("USER");
            user.setIsaccount("Y");    // 改為 "Y"，與其他方法一致
            user.setIsblacklist("N");  // 改為 "N"，與其他方法一致

            // ✅ 儲存到資料庫
            UserEntity savedUser = userRepository.save(user);
            System.out.println("✅ 註冊成功，用戶 ID: " + savedUser.getUserid());

            return true;

        } catch (Exception e) {
            System.err.println("❌ 註冊過程發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ✅ 新增：建立用戶方法（供 Controller 直接使用）
    public UserEntity createUser(RegisterRequest request) {
        System.out.println("🔨 開始建立用戶: " + request.getAccountemail());

        // 輸入驗證
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能為空");
        }

        if (request.getAccountemail() == null || request.getAccountemail().trim().isEmpty()) {
            throw new IllegalArgumentException("信箱不能為空");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("密碼至少需要6個字元");
        }

        // 檢查重複
        if (existsByEmail(request.getAccountemail())) {
            throw new IllegalArgumentException("此信箱已被註冊");
        }

        if (existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("此用戶名已被使用");
        }

        try {
            UserEntity user = new UserEntity();
            user.setUsername(request.getUsername().trim());
            user.setAccountemail(request.getAccountemail().trim().toLowerCase());
            user.setPhonenumber(request.getPhonenumber() != null ? request.getPhonenumber().trim() : null);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole("USER");
            user.setIsaccount("1");
            user.setIsblacklist("0");

            UserEntity savedUser = userRepository.save(user);
            System.out.println("✅ 用戶建立成功: ID=" + savedUser.getUserid());

            return savedUser;

        } catch (Exception e) {
            System.err.println("❌ 用戶建立失敗: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("用戶建立失敗", e);
        }
    }

    // ✅ 新增：檢查方法
    public boolean existsByEmail(String email) {
        return userRepository.findByAccountemail(email).isPresent();
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    // ✅ 修正：findByAccountemail 回傳 Optional 處理
    @Override
    public UserEntity findByAccountemail(String accountemail) {
        Optional<UserEntity> userOpt = userRepository.findByAccountemail(accountemail);
        return userOpt.orElse(null);
    }

    // ✅ 新增：findByUsername 方法
    public UserEntity findByUsername(String username) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        return userOpt.orElse(null);
    }

    // ✅ 修正：updateUserByEmail 使用 Optional，支援多選欄位
    // ✅ 確保方法簽名一致
    @Override
    public boolean updateUserByEmail(String email, UserEntity formUser) {
        Optional<UserEntity> userOpt = userRepository.findByAccountemail(email);
        if (userOpt.isEmpty()) return false;

        UserEntity user = userOpt.get();
        try {
            BeanUtils.copyProperties(formUser, user,
                    "userid", "accountemail", "password", "role", "isaccount", "isblacklist", "createdAt");

            if (formUser.getPetList() != null) user.setPetList(formUser.getPetList());
            if (formUser.getPetActivitiesList() != null) user.setPetActivitiesList(formUser.getPetActivitiesList());

            userRepository.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ 新增一個 userId 更新
    @Override
    public boolean updateUser(Long userId, UserEntity formUser) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;

        UserEntity user = userOpt.get();
        try {
            BeanUtils.copyProperties(formUser, user,
                    "userid", "accountemail", "password", "role", "isaccount", "isblacklist", "createdAt");

            if (formUser.getPetList() != null) user.setPetList(formUser.getPetList());
            if (formUser.getPetActivitiesList() != null) user.setPetActivitiesList(formUser.getPetActivitiesList());

            userRepository.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }





    // ✅ 修正：changePassword 使用 Optional
    @Override
    public String changePassword(String email, String currentPassword, String newPassword, String confirmPassword) {
        Optional<UserEntity> userOpt = userRepository.findByAccountemail(email);

        if (userOpt.isEmpty()) {
            return "帳號不存在";
        }

        UserEntity user = userOpt.get();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return "目前密碼不正確";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "新密碼與確認密碼不一致";
        }

        if (newPassword.length() < 6) {
            return "新密碼至少需要6個字元";
        }

        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            System.out.println("✅ 密碼更新成功: " + email);
            return "密碼更新成功";

        } catch (Exception e) {
            System.err.println("❌ 密碼更新失敗: " + e.getMessage());
            return "密碼更新失敗";
        }
    }

    // ✅ Google 登入存進資料庫
    public void saveUser(UserEntity user) {
        try {

            userRepository.save(user);
            System.out.println("✅ 用戶儲存成功: " + user.getAccountemail());
        } catch (Exception e) {
            System.err.println("❌ 用戶儲存失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }
}