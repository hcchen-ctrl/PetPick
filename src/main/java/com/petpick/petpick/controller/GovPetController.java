package com.petpick.petpick.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petpick.petpick.model.PetDTO;



@RestController
@RequestMapping("/api")
public class GovPetController {

    // ✅ 👇 放在這裡（類別裡但方法外）
    private static List<PetDTO> cachedPets = null;
    private static long lastFetchTime = 0;
    private static final long CACHE_DURATION = 1000 * 60 * 10; // 10 分鐘

    private void ensureCache() throws IOException {long now = System.currentTimeMillis();
    if (cachedPets == null || now - lastFetchTime > CACHE_DURATION) {
        System.out.println("⬇️ 重新抓政府 API");
        String url = "https://data.moa.gov.tw/Service/OpenData/TransService.aspx?UnitId=QcbUEzN6E6DL&IsTransData=1";
        URL apiUrl = new URL(url);
        BufferedReader reader = new BufferedReader(new InputStreamReader(apiUrl.openStream(), "UTF-8"));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        ObjectMapper mapper = new ObjectMapper();
        cachedPets = Arrays.asList(mapper.readValue(sb.toString(), PetDTO[].class));
        lastFetchTime = now;
    }
}

    
    private String translateBodytype(String type) {
        return switch (type) {
            case "SMALL" -> "小型";
            case "MEDIUM" -> "中型";
            case "BIG" -> "大型";
            default -> type;
        };
    }

    private String translateAge(String age) {
        return switch (age) {
            case "CHILD" -> "幼年";
            case "ADULT" -> "成年";
            default -> "不明";
        };
    }

    @GetMapping("/shelters")
    public Set<String> getAllShelters() throws IOException {
    ensureCache(); // 確保 cachedPets 有資料
    return cachedPets.stream()
            .map(p -> p.animal_place)
            .filter(place -> place != null && !place.isBlank())
            .collect(TreeSet::new, Set::add, Set::addAll); // 自動排序 + 去重
}

@GetMapping("/kinds")
public Set<String> getAllKinds() throws IOException {
    ensureCache();
    return cachedPets.stream()
            .map(p -> p.animal_kind)
            .filter(kind -> kind != null && !kind.isBlank())
            .collect(TreeSet::new, Set::add, Set::addAll);
}

@GetMapping("/sexes")
public Set<String> getAllSexes() throws IOException {
    ensureCache();
    return cachedPets.stream()
            .map(p -> p.animal_sex)
            .filter(sex -> sex != null && !sex.isBlank())
            .map(String::toUpperCase)
            .collect(TreeSet::new, Set::add, Set::addAll);
}

@GetMapping("/ages")
public Set<String> getAllAges() throws IOException {
    ensureCache();
    return cachedPets.stream()
            .map(p -> p.animal_age)
            .filter(age -> age != null && !age.isBlank())
            .map(String::toUpperCase)
            .collect(TreeSet::new, Set::add, Set::addAll);
}

    @GetMapping("/pets")
    public Map<String, Object> getGovPets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String shelter,
            @RequestParam(required = false) String sex,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean fullSearch
    ) throws IOException {
        ensureCache();

        List<PetDTO> filtered = cachedPets.stream()
                .filter(p -> kind == null || kind.isEmpty() || p.animal_kind.contains(kind))
                .filter(p -> shelter == null || shelter.isEmpty() || p.animal_place.contains(shelter))
                .filter(p -> sex == null || sex.isBlank() || p.animal_sex != null && p.animal_sex.toLowerCase().contains(sex.toLowerCase()))
                .filter(p -> age == null || age.isBlank() || 
                        (p.animal_age != null && p.animal_age.toUpperCase().equals(age.toUpperCase())))
                .filter(p -> {
                        if (keyword == null || keyword.isBlank()) return true;

                        StringBuilder sb = new StringBuilder();

                        if (Boolean.TRUE.equals(fullSearch)) {
                            // 廣泛搜尋：全部欄位都進去
                            sb.append(p.animal_remark == null ? "" : p.animal_remark)
                            .append(p.animal_Variety == null ? "" : p.animal_Variety)
                            .append(p.animal_colour == null ? "" : p.animal_colour)
                            .append(p.animal_subid == null ? "" : p.animal_subid)
                            .append(p.animal_place == null ? "" : p.animal_place)
                            .append(p.shelter_tel == null ? "" : p.shelter_tel)
                            .append(p.shelter_address == null ? "" : p.shelter_address)
                            .append(translateBodytype(p.animal_bodytype))
                            .append(translateAge(p.animal_age));
                        } else {
                            // 精準搜尋：僅分類欄位
                            sb.append(p.animal_Variety == null ? "" : p.animal_Variety)
                            .append(p.animal_colour == null ? "" : p.animal_colour)
                            .append(p.animal_kind == null ? "" : p.animal_kind)
                            .append(translateBodytype(p.animal_bodytype))
                            .append(translateAge(p.animal_age));
                        }

                        String combined = sb.toString().replaceAll("\\s+", "");
                        if (keyword.length() >= 4 || keyword.matches(".*\\d.*")) {
                            return combined.contains(keyword);
                        }

                        return keyword.chars()
                                    .mapToObj(c -> String.valueOf((char) c))
                                    .allMatch(combined::contains);
                    })  
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<PetDTO> pageData = (fromIndex >= filtered.size()) ? List.of() : filtered.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("content", pageData);
        result.put("number", page);
        result.put("totalPages", (int) Math.ceil((double) filtered.size() / size));
        return result;
    }


}


