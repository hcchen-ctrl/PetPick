package com.petpick.petpick.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    @Value("${petpick.upload-dir}")
    private String adoptUploadDir;   // 例如 ./data/adopt-uploads

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 舊有 /uploads/** 若你們其他功能有用到就保留，沒有可以刪掉
        Path projectUploads = Paths.get("uploads").toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(projectUploads.toUri().toString());

        // ★ 認養貼文圖：從「外部資料夾」提供靜態檔案，不再用 classpath
        String adoptLocation = Paths.get(adoptUploadDir)
                                    .toAbsolutePath()
                                    .normalize()
                                    .toUri()
                                    .toString();   // e.g. file:/C:/xxx/data/adopt-uploads/
        registry.addResourceHandler("/adopt/uploads/**")
                .addResourceLocations(adoptLocation)
                .setCachePeriod(3600);

        // （可選）如果你有 feedback 圖，也可在這裡加外部資料夾映射
        Path feedbackDir = Paths.get("uploads/feedback").toAbsolutePath().normalize();
        registry.addResourceHandler("/adopt/feedback/**")
                .addResourceLocations(feedbackDir.toUri().toString());
    }
}
