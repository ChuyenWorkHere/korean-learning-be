package edu.language.kbee.repository;

import edu.language.kbee.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import edu.language.kbee.model.SubscriptionPackage;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    boolean existsByTxnRef(String txnRef);

    @Query("SELECT COALESCE(SUM(sp.price), 0) FROM PaymentTransaction pt JOIN SubscriptionPackage sp ON pt.packageId = sp.id")
    BigDecimal calculateTotalRevenue();

    List<PaymentTransaction> findByUsernameOrderByCreatedAtDesc(String username);
}
