package com.example.hcc.dto;

import com.example.hcc.enums.WorkUnitStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class WorkUnitResponse {

    private Long id;
    private String pageStart;
    private String pageEnd;
    private WorkUnitStatus status;

    private List<UserResponse> assignedTo;
}
