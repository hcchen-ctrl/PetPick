package com.petpick.petpick.service;


import com.petpick.petpick.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    @Autowired
    private UserService userService; // 你自己寫的服務

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 取得 Google 傳回的 user info
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // 檢查資料庫是否已有使用者
        UserEntity user = userService.findByAccountemail(email);
        if (user == null) {
            // 如果沒有就建立一個新使用者（可加預設密碼或標記是Google用戶）
            UserEntity newUser = new UserEntity();
            newUser.setAccountemail(email);
            newUser.setUsername(name); // 假設你有 name 欄位
            newUser.setRole("USER"); // 或依你設計
            userService.saveUser(newUser);// 自己的 save 方法
        }

        return oAuth2User;
    }

}
