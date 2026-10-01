package com.meetgrid.repository;
import com.meetgrid.model.PaymentSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentSubscriptionRepository extends JpaRepository<PaymentSubscription,String> {
 Optional<PaymentSubscription> findFirstByOwnerIdOrderByCreatedAtDesc(String ownerId);
 @org.springframework.data.jpa.repository.Query("select s.ownerId from PaymentSubscription s where s.id = :id")
 Optional<String> findOwnerById(@org.springframework.data.repository.query.Param("id") String id);
 java.util.List<PaymentSubscription> findTop20ByStatusInAndSyncedAtBeforeOrderBySyncedAtAsc(java.util.Collection<String> statuses,java.time.Instant before);
}
