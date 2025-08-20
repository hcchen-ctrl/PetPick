package com.petpick.petpick_server.controller;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick_server.entity.UserInfo;
import com.petpick.petpick_server.repository.UserinfoRepository;

@RestController
@RequestMapping("/api/users")
public class UserinfoController {

    @Autowired
    private UserinfoRepository userinfoRepository;

    @GetMapping("/avatar/{id}")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable long id) throws IOException {
        Optional<UserInfo> optionalUser = userinfoRepository.findById(id);
        if (!optionalUser.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        UserInfo user = optionalUser.get();
        byte[] imageBytes;

        if (user.getIcon() != null) {
            imageBytes = user.getIcon(); 
        } else {
            ClassPathResource defaultImage = new ClassPathResource("static/finalProject/images/default-avatar.png");
            imageBytes = StreamUtils.copyToByteArray(defaultImage.getInputStream());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }
}