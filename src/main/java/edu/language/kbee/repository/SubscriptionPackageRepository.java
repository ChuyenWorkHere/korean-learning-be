package edu.language.kbee.repository;

import edu.language.kbee.entity.SubscriptionPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPackageRepository extends JpaRepository<SubscriptionPackage, Long> {

    List<SubscriptionPackage> findByIsActiveTrue();
    
    Optional<SubscriptionPackage> findByName(String name);
}
