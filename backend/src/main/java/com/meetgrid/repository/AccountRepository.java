package com.meetgrid.repository;
import com.meetgrid.model.Account;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface AccountRepository extends JpaRepository<Account,String> {
 Optional<Account> findByEmail(String email);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select a from Account a where a.id = :id")
 Optional<Account> lockById(@org.springframework.data.repository.query.Param("id") String id);
}
