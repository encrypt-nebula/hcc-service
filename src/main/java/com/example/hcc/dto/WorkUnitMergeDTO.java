package com.example.hcc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class WorkUnitMergeDTO {
    private Long id;
    private LocalDate dos; // or Integer if your `dos` is integer
}
