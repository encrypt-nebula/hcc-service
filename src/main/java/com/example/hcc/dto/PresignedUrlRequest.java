package com.example.hcc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PresignedUrlRequest {
    private String s3Path;
}
