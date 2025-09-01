package com.petpick.petpick.config;

// 方法一：建立 CorsConfig.java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 允許所有 /api/** 的路徑
                .allowedOrigins(
                        "http://localhost:5173",   // Vite 開發伺服器
                        "http://127.0.0.1:5173",
                        "http://localhost:3000"    // 備用端口
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // 預檢請求快取時間
    }
}

// 或者方法二：在您的控制器類別上加入註解
/*
@RestController
@RequestMapping("/api")
@CrossOrigin(
    origins = {"http://localhost:5173", "http://127.0.0.1:5173"},
    allowedHeaders = "*",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class YourController {
    // ... 您的控制器方法
}
*/