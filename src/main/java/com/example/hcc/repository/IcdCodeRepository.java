package com.example.hcc.repository;

import com.example.hcc.entity.IcdCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IcdCodeRepository extends JpaRepository<IcdCode, Long> {

    Optional<IcdCode> findByIcdCode(String icdCode);

    List<IcdCode> findByIcdCodeIn(List<String> icdCodes);
}

