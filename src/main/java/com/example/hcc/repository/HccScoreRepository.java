package com.example.hcc.repository;

import com.example.hcc.entity.HccScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HccScoreRepository extends JpaRepository<HccScore, Long> {
    List<HccScore> findAllByIcdCodeIn(List<String> icdCodes);
}
