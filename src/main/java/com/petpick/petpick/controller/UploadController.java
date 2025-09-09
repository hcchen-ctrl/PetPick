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
@RequestMapping("/api/adopts")  // ← 改成有 /api
public class UploadController {

    @Value("${petpick.upload-dir}")
    private String uploadDir;

    @Value("${petpick.upload-url-prefix:/adopt/uploads/}")
    private String urlPrefix;

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("files") List<MultipartFile> files) throws Exception {
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);

        List<String> urls = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f.isEmpty()) continue;

            // 先用檔名取 ext，取不到就看 content-type
            String ext = Optional.ofNullable(f.getOriginalFilename())
                    .filter(n -> n.contains("."))
                    .map(n -> n.substring(n.lastIndexOf(".")).toLowerCase())
                    .orElse("");

            if (ext.isBlank()) {
                String ct = Optional.ofNullable(f.getContentType()).orElse("");
                if (ct.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE)) ext = ".jpg";
                else if (ct.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)) ext = ".png";
                else if (ct.equalsIgnoreCase(MediaType.IMAGE_GIF_VALUE)) ext = ".gif";
                else ext = ".bin"; // 最後保底
            }

            String filename = UUID.randomUUID() + ext;
            Path dest = root.resolve(filename);
            f.transferTo(dest.toFile());

            String prefix = urlPrefix.endsWith("/") ? urlPrefix : urlPrefix + "/";
            urls.add(prefix + filename);
        }
        return Map.of("urls", urls);
    }
}
