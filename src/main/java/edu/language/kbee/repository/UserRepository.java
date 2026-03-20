package edu.language.kbee.repository;

import edu.language.kbee.enums.RoleName;
import edu.language.kbee.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRoles_RoleNameEquals(RoleName roleName);
}
