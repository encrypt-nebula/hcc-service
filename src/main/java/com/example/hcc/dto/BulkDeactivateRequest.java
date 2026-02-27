package com.example.hcc.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BulkDeactivateRequest {

    @NotEmpty(message = "ids must not be empty")
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}