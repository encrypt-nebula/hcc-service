package com.example.hcc.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IcdEntry {
    private String code;

    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    @JsonProperty("reason")
    public void setReason(String reason) {
        if (reason != null && !reason.isBlank()) {
            if (this.reasons == null) {
                this.reasons = new ArrayList<>();
            }
            this.reasons.add(reason);
        }
    }

    @JsonCreator
    public static IcdEntry fromString(String code) {
        return IcdEntry.builder()
                .code(code)
                .reasons(new ArrayList<>())
                .build();
    }
}
