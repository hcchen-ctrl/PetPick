package com.petpick.petpick.service.mission;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.petpick.petpick.DTO.mission.MissionDetailDTO;
import com.petpick.petpick.DTO.mission.MissionUploadRequest;
import com.petpick.petpick.entity.mission.Mission;
import com.petpick.petpick.entity.mission.MissionImage;
import com.petpick.petpick.entity.mission.Tag;
import com.petpick.petpick.entity.mission.UserInfo;
import com.petpick.petpick.repository.mission.MissionImageRepository;
import com.petpick.petpick.repository.mission.MissionRepository;
import com.petpick.petpick.repository.mission.TagRepository;
import com.petpick.petpick.repository.mission.UserinfoRepository;

@Service
public class MissionUploadService {
    private final MissionRepository missionRepo;
    private final TagRepository tagRepo;
    private final UserinfoRepository userRepo;
    private final MissionImageRepository imageRepo;
    private final FileStorageService fileStorage;

    public MissionUploadService(
            MissionRepository missionRepo,
            UserinfoRepository userRepo,
            TagRepository tagRepo,
            MissionImageRepository imageRepo,
            FileStorageService fileStorage) {
        this.missionRepo = missionRepo;
        this.userRepo = userRepo;
        this.tagRepo = tagRepo;
        this.imageRepo = imageRepo;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public MissionDetailDTO createMission(MissionUploadRequest req, List<MultipartFile> images) {
        UserInfo poster = userRepo.findById(req.posterId)
                .orElseThrow(() -> new IllegalArgumentException("Poster not found: " + req.posterId));

        Mission m = new Mission();
        m.setPoster(poster);
        m.setTitle(req.title);
        m.setDescription(req.description);
        m.setCity(req.city);
        m.setDistrict(req.district);
        m.setStartTime(req.startTime);
        m.setEndTime(req.endTime);
        m.setPrice(req.price);
        m.setPetName(req.petName);
        m.setPetAge(req.petAge);
        m.setPetGender("公".equals(req.petGender) ? Mission.PetGender.公 : Mission.PetGender.母);
        m.setContactPhone(req.contactPhone);

        if (req.tags != null && !req.tags.isEmpty()) {
            List<Tag> tags = tagRepo.findAllById(req.tags);
            for (Tag tag : tags) {
                m.getTags().add(tag);
            }
        }

        Mission saved = missionRepo.saveAndFlush(m);

        List<MissionImage> toSave = new ArrayList<>();
        if (images != null) {
            for (MultipartFile f : images) {
                if (f.isEmpty())
                    continue;
                String url = fileStorage.saveMissionImage(saved.getMissionId(), f);

                MissionImage mi = new MissionImage();
                mi.setMission(saved);
                mi.setImageUrl(url);
                toSave.add(mi);
            }
            if (!toSave.isEmpty())
                imageRepo.saveAll(toSave);
        }
        missionRepo.save(saved);

        Mission fullMission = missionRepo.findWithAllByMissionId(saved.getMissionId())
                .orElseThrow(() -> new IllegalStateException("Mission not found after save"));

        return new MissionDetailDTO(fullMission);
    }

}
