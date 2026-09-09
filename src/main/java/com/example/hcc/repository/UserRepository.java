package com.example.hcc.repository;

import com.example.hcc.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Optional<User> findByCognitoId(String cognitoId);

    List<User> findByRole(com.example.hcc.enums.Role role);

    @Query("SELECT u.id FROM User u WHERE u.role = :role")
    List<Long> findIdsByRole(@Param("role") com.example.hcc.enums.Role role);

    @Query("SELECT u.id FROM User u WHERE u.role = :role AND u.company.id = :companyId")
    List<Long> findIdsByRoleAndCompanyId(
            @Param("role") com.example.hcc.enums.Role role,
            @Param("companyId") Long companyId
    );

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT u FROM User u")
    List<User> findAllWithCompany();

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT u FROM User u WHERE u.company.id = :companyId")
    List<User> findByCompanyIdWithCompany(@Param("companyId") Long companyId);
}
