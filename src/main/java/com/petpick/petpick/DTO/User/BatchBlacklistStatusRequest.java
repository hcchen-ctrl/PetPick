package com.petpick.petpick.DTO.User;

import java.util.List;

public class BatchBlacklistStatusRequest {
    private List<Long> userids;
    private String isblacklist; // 跟 Entity 對齊：String
    private String reason;

    public List<Long> getUserids() {
        return userids;
    }

    public void setUserids(List<Long> userids) {
        this.userids = userids;
    }

    public String getIsblacklist() {
        return isblacklist;
    }

    public void setIsblacklist(String isblacklist) {
        this.isblacklist = isblacklist;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

