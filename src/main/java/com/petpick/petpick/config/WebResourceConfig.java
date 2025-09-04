package com.petpick.petpick.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    /** 專案根 (…/petpick) */
    private Path projectRoot() {
        try {
            return Paths.get(WebResourceConfig.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParent()   // /target
                    .getParent()   // 專案根
                    .normalize();
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadsRoot = projectRoot().resolve("uploads").normalize();
        Path feedbackDir = uploadsRoot.resolve("feedback").normalize();

        // 1) 舊路徑保留：/uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsRoot.toUri().toString());

        // 2) 認養貼文圖（打包在 classpath）
        registry.addResourceHandler("/adopt/uploads/**")
                .addResourceLocations("classpath:/static/adopt/uploads/");

        // 3) 回報圖：正式 /adopt/feedback/** ，同時相容 /adopt/uploads/feedback/**
        registry.addResourceHandler("/adopt/feedback/**", "/adopt/uploads/feedback/**")
                .addResourceLocations(
                        feedbackDir.toUri().toString(),                 // 實體 uploads/feedback
                        "classpath:/static/adopt/feedback/"             // fallback（預設圖）
                );

        try {
            System.out.printf("[WebResourceConfig] /uploads/**               -> %s (exists=%s)%n",
                    uploadsRoot, Files.exists(uploadsRoot));
            System.out.printf("[WebResourceConfig] /adopt/feedback/**       -> %s (exists=%s)%n",
                    feedbackDir, Files.exists(feedbackDir));
            System.out.println("[WebResourceConfig] /adopt/uploads/**        -> classpath:/static/adopt/uploads/");
        } catch (Exception ignore) {}
    }
}
