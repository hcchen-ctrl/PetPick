package com.petpick.petpick.service;

import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.repository.UserRepository;
import com.petpick.petpick.repository.UserRepositoryV2;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceV2 {

    private final UserRepositoryV2 userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceV2(UserRepositoryV2 userRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    // 搜尋
    // 分頁搜尋會員
    public Page<UserEntity> searchUsers(String q, String isaccount, String isblacklist, String role, Pageable pageable) {
        return userRepository.findAll(UserSpecification.buildSearchSpec(q, isaccount, isblacklist, role), pageable);
    }

    // 更新資料
    public void updateUser(Long userid, UserEntity updateData) {
        UserEntity user = userRepository.findById(userid)
                .orElseThrow(() -> new EntityNotFoundException("找不到會員 ID: " + userid));

        if (updateData.getUsername() != null) user.setUsername(updateData.getUsername());
        if (updateData.getAccountemail() != null) user.setAccountemail(updateData.getAccountemail());
        if (updateData.getPhonenumber() != null) user.setPhonenumber(updateData.getPhonenumber());
        if (updateData.getGender() != null) user.setGender(updateData.getGender());
        if (updateData.getCity() != null) user.setCity(updateData.getCity());
        if (updateData.getDistrict() != null) user.setDistrict(updateData.getDistrict());
        if (updateData.getRole() != null) user.setRole(updateData.getRole());
        if (updateData.getIsaccount() != null) user.setIsaccount(updateData.getIsaccount());
        if (updateData.getIsblacklist() != null) user.setIsblacklist(updateData.getIsblacklist());
        if (updateData.getExperience() != null) user.setExperience(updateData.getExperience());
        if (updateData.getDaily() != null) user.setDaily(updateData.getDaily());

        userRepository.save(user);
    }

    // 重設密碼
    public void resetPassword(Long userid, String newPassword) {
        UserEntity user = userRepository.findById(userid)
                .orElseThrow(() -> new EntityNotFoundException("找不到會員 ID: " + userid));

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    // 批次更新帳號狀態
    public void batchUpdateAccountStatus(List<Long> userids, String isaccount, String reason) {
        List<UserEntity> users = userRepository.findAllById(userids);
        for (UserEntity user : users) {
            user.setIsaccount(isaccount);
        }
        userRepository.saveAll(users);
    }

    // 批次更新黑名單
    public void batchUpdateBlacklistStatus(List<Long> userids, String isblacklist, String reason) {
        List<UserEntity> users = userRepository.findAllById(userids);
        for (UserEntity user : users) {
            user.setIsblacklist(isblacklist);
        }
        userRepository.saveAll(users);
    }

    // 軟刪除
    public void deleteUser(Long userid, String reason) {
        UserEntity user = userRepository.findById(userid)
                .orElseThrow(() -> new EntityNotFoundException("找不到會員 ID: " + userid));

        user.setIsblacklist("Y");
        user.setIsaccount("N");
        userRepository.save(user);

        // 硬刪除的話：
        // userRepository.delete(user);
    }
}
