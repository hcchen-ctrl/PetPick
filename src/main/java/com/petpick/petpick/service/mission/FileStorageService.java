package com.petpick.petpick.service.mission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path missionRoot;

    public FileStorageService(
        @Value("${petpick.upload.mission-dir}") String missionDir
    ) {
        this.missionRoot = Paths.get(missionDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.missionRoot);
        } catch (IOException e) {
            throw new RuntimeException("無法建立上傳目錄：" + missionRoot, e);
        }
    }

    
    public String saveMissionImage(Long missionId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + (ext.isEmpty() ? "" : "." + ext);

        // 檔案存放路徑
        Path missionDir = missionRoot.resolve(String.valueOf(missionId));
        try {
            Files.createDirectories(missionDir);
            Path target = missionDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return "/finalProject/mission/missionsImg/" + missionId + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("檔案儲存失敗：" + filename, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? "" : filename.substring(dot + 1);
    }
}
