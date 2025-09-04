package com.petpick.petpick.controller.mission;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petpick.petpick.DTO.mission.MissionDetailDTO;
import com.petpick.petpick.DTO.mission.MissionUploadRequest;
import com.petpick.petpick.service.mission.MissionUploadService;



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
            @RequestPart("data") String data,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); 
        MissionUploadRequest dto = mapper.readValue(data, MissionUploadRequest.class);

        return missionUploadService.createMission(dto, images);
    }

}
