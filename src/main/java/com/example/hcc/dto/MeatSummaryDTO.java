package com.example.hcc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeatSummaryDTO {
    private Long workUnitId;
    private Boolean monitor;
    private Boolean evaluate;
    private Boolean assessOrAddress;
    private Boolean treat;
    private Map<String, List<String>> categories;
}
