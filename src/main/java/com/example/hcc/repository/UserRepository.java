package com.example.hcc.repository;

import com.example.hcc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Optional<User> findByCognitoId(String cognitoId);

    java.util.List<User> findByRole(com.example.hcc.enums.Role role);

    @org.springframework.data.jpa.repository.Query("SELECT u.id FROM User u WHERE u.role = :role")
    java.util.List<Long> findIdsByRole(@org.springframework.data.repository.query.Param("role") com.example.hcc.enums.Role role);
}

