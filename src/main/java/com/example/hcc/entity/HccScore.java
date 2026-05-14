package com.example.hcc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "hcc_scores")
@Getter
@Setter
public class HccScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "icd_code")
    private String icdCode;

    @Column(name = "hcc_score", precision = 12, scale = 4)
    private BigDecimal hccScore;
}

