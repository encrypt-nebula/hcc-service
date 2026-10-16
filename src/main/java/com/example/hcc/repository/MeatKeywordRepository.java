package com.example.hcc.repository;

import com.example.hcc.entity.MeatKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeatKeywordRepository extends JpaRepository<MeatKeyword, Long> {
    List<MeatKeyword> findByWorkUnitId(Long workUnitId);

    List<MeatKeyword> findByWorkUnit_IdIn(List<Long> workUnitIds);

    void deleteByWorkUnitId(Long workUnitId);
}
