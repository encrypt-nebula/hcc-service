package com.example.hcc.dto;

import lombok.Data;
import java.util.List;

@Data
public class IcdValidationRequest {
    private List<String> queries;
}
