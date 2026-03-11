package com.example.hcc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageRangeRequest {

    private Long fileId;
    private String pageRange;

}
