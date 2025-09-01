package tw.petpick.petpick.dto;

import java.time.LocalDate;

/** 介面投影：Repository 的 native SQL 要用 AS 對到這些方法名 */
public interface AdoptionView {
    Long getId();
    String getOwnerName();
    String getPetName();
    LocalDate getAdoptionDate();   // 若你想回傳字串也可以改成 String
    String getImageUrl();
    Integer getRequiredReports();
    Long getReportCount();
}
