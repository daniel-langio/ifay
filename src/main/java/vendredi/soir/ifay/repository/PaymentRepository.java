package vendredi.soir.ifay.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vendredi.soir.ifay.model.Provider;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

  /**
   * Pessimistic-locked lookup by the one thing a claim and a report must agree on - callers must
   * hold this lock for the rest of their transaction while deciding whether to create a new
   * payment or update the existing (other-side) one, so two concurrent reports for the same
   * pspRef can never race into two separate rows instead of matching each other.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PaymentEntity p where p.type = :type and p.pspRef = :pspRef")
  Optional<PaymentEntity> findByTypeAndPspRefForUpdate(
      @Param("type") Provider type, @Param("pspRef") String pspRef);
}
