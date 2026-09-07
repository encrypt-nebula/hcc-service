package com.example.hcc.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoderActivityFunnelDto {
    private long loggedIn;
    private long assigned;
    private long submitted;
    private long pending;
}
