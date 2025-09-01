package tw.petpick.petpick.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${petpick.upload-dir:uploads}")            // ← 沒設時用專案根下 /uploads
    private String uploadDir;

    @Value("${petpick.upload-url-prefix:/uploads/}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        // 例如：/uploads/** → file:/C:/yourproj/uploads/ 或 file:/home/you/yourproj/uploads/
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations("file:" + root.toString() + "/");
    }
}
