package com.example.hcc.dto;

import com.example.hcc.enums.Status;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkStatusRequest {

    @NotEmpty(message = "ids must not be empty")
    private List<Long> ids;

    @NotNull(message = "status must not be null")
    private Status status;
}