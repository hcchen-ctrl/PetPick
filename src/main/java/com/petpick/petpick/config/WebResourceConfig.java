package tw.petpick.petpick.config;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    private java.nio.file.Path moduleRoot() {
        try {
            // .../petpick/target/classes/ → getParent()=.../target →
            // getParent()=.../petpick
            return java.nio.file.Paths.get(
                    getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().getParent()
                    .normalize();
        } catch (Exception e) {
            return java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
    }

    @Override
    public void addResourceHandlers(
            org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        java.nio.file.Path uploadsRoot = moduleRoot().resolve("uploads").normalize();
        String location = uploadsRoot.toUri().toString(); 
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);

        // 啟動時印出來，方便你核對
        try {
            System.out.printf("[WebResourceConfig] map /uploads/** -> %s (exists=%s)%n",
                    uploadsRoot, java.nio.file.Files.exists(uploadsRoot));
        } catch (Exception ignore) {
        }
    }

}