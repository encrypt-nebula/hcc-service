package com.example.hcc.repository;

import com.example.hcc.entity.IcdCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IcdCodeRepository extends JpaRepository<IcdCode, Long> {

    Optional<IcdCode> findByIcdCode(String icdCode);

    List<IcdCode> findByIcdCodeIn(List<String> icdCodes);

    @org.springframework.data.jpa.repository.Query("SELECT c.icdCode FROM IcdCode c WHERE LOWER(c.icdCode) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findValidCodesByQuery(@org.springframework.data.repository.query.Param("query") String query);
}
