package com.petpick.petpick.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/adopts")
public class UploadController {

    @Value("${petpick.upload-dir}")
    private String uploadDir;

    @Value("${petpick.upload-url-prefix:/uploads/}")
    private String urlPrefix;

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("files") List<MultipartFile> files) throws Exception {
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();

        // 建立資料夾
        Files.createDirectories(root);

        List<String> urls = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f.isEmpty()) continue;

            // 擷取副檔名
            String ext = Optional.ofNullable(f.getOriginalFilename())
                    .filter(n -> n.contains("."))
                    .map(n -> n.substring(n.lastIndexOf(".")))
                    .orElse("");

            // 產生唯一檔名
            String filename = UUID.randomUUID() + ext;
            Path dest = root.resolve(filename);

            // 儲存圖片到 static/adopt/upload
            f.transferTo(dest.toFile());

            // 回傳對應網址
            urls.add("/adopt/uploads/" + filename);

        }

        return Map.of("urls", urls);
    }
}