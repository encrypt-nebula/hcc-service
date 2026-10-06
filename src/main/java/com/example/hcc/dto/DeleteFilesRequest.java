package com.example.hcc.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteFilesRequest {

    @NotEmpty(message = "fileIds must not be empty")
    private List<Long> fileIds;
}
