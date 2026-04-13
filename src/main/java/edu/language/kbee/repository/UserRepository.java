package edu.language.kbee.repository;

import edu.language.kbee.enums.RoleName;
import edu.language.kbee.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRoles_RoleNameEquals(RoleName roleName);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName " +
           "ORDER BY CASE WHEN u.lastActivityDate >= :yesterday THEN u.currentStreak ELSE 0 END DESC")
    List<User> findTopUsersByRealStreak(
            @Param("roleName") RoleName roleName,
            @Param("yesterday") LocalDate yesterday,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isPremium = 1")
    long countPremiumUsers();
}
