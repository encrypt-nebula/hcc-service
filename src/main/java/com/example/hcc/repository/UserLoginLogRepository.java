package com.example.hcc.repository;

import com.example.hcc.entity.UserLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {

    @Query("SELECT COUNT(DISTINCT l.userId) FROM UserLoginLog l WHERE l.loginTime >= :startTime AND l.loginTime <= :endTime")
    long countDistinctLoginsInPeriod(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(DISTINCT l.userId) FROM UserLoginLog l WHERE l.userId IN :userIds AND l.loginTime >= :startTime AND l.loginTime <= :endTime")
    long countDistinctLoginsInPeriodForUsers(@Param("userIds") List<Long> userIds, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(l) FROM UserLoginLog l WHERE l.userId = :userId AND l.loginTime >= :startTime AND l.loginTime <= :endTime")
    long countLoginsForUserInPeriod(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
