package com.example.hcc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "icd_codes")
@Getter
@Setter
public class IcdCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "icd_code")
    private String icdCode;

    @Column(name = "hcc_score")
    private BigDecimal hccScore;

    private String description;
}
