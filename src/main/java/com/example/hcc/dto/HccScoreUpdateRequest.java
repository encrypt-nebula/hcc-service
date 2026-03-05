package com.example.hcc.dto;

import lombok.Data;
import java.util.List;

@Data
public class HccScoreUpdateRequest {
    private List<String> icdCodes;
    private Long codingResultId;
}
