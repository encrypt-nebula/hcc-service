package com.example.hcc.dto;

import com.example.hcc.entity.IcdEntry;
import lombok.Data;
import java.util.List;

@Data
public class HccScoreUpdateRequest {
    private List<IcdEntry> icdCodes;
    private Long codingResultId;
}
