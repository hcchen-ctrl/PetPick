package com.petpick.petpick.DTO.User;

import java.util.List;

public class BatchAccountStatusRequest {
    private List<Long> userids;
    private String isaccount; // 跟 Entity 對齊：String
    private String reason;

    public List<Long> getUserids() {
        return userids;
    }

    public void setUserids(List<Long> userids) {
        this.userids = userids;
    }

    public String getIsaccount() {
        return isaccount;
    }

    public void setIsaccount(String isaccount) {
        this.isaccount = isaccount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

