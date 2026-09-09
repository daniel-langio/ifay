package vendredi.soir.ifay.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vendredi.soir.ifay.model.Provider;

/**
 * No locking here: every write path goes through {@code PaymentTransactions}, which locks the
 * global {@link ChainTipEntity} row first - that already serializes every ledger write to one at
 * a time, so a second lock on the Payment row itself would be redundant.
 */
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
  Optional<PaymentEntity> findByTypeAndPspRef(Provider type, String pspRef);
}
