package com.example.hcc.controller;

import com.example.hcc.dto.MeatSummaryDTO;
import com.example.hcc.entity.MeatKeyword;
import com.example.hcc.service.WorkUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meat-keywords")
@RequiredArgsConstructor
public class MeatKeywordController {

    private final WorkUnitService workUnitService;

    @GetMapping("/work-unit/{workUnitId}")
    public List<MeatKeyword> getKeywordsByWorkUnit(@PathVariable Long workUnitId) {
        return workUnitService.getMeatKeywords(workUnitId);
    }

    @GetMapping("/work-unit/{workUnitId}/summary")
    public MeatSummaryDTO getSummaryByWorkUnit(@PathVariable Long workUnitId) {
        return workUnitService.getMeatSummary(workUnitId);
    }
}
