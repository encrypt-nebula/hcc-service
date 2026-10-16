package com.example.hcc.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meat_keywords")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MeatKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "work_unit_id")
    private WorkUnit workUnit;

    @Column(name = "category", length = 50)
    private String category; // MONITORING, EVALUATION, ASSESSMENT, TREATMENT

    @Column(name = "keyword", length = 255)
    private String keyword;
}
