package com.example.hcc.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssignWorkUnitsRequest {
    private List<PageRangeRequest> pageRangeRequest;

}