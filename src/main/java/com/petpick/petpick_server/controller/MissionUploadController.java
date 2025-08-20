package com.petpick.petpick_server.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.petpick.petpick_server.dto.MissionDetailDTO;
import com.petpick.petpick_server.dto.MissionUploadRequest;
import com.petpick.petpick_server.service.MissionUploadService;

@RestController
@RequestMapping("/api/missions")
public class MissionUploadController {
    private final MissionUploadService missionUploadService;

    public MissionUploadController(MissionUploadService missionUploadService) {
        this.missionUploadService = missionUploadService;
    }

    @PostMapping(
        value = "/upload", 
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public MissionDetailDTO upload(
            @RequestPart("data") MissionUploadRequest data,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
            ) 
            throws IOException {
        return missionUploadService.createMission(data, images);
    }
}
