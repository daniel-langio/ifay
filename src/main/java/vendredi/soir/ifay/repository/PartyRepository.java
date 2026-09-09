package vendredi.soir.ifay.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * No locking here on purpose: every write path that resolves a Party does so from inside a
 * transaction that already holds the global {@link ChainTipEntity} lock (see
 * {@code PaymentTransactions}), which serializes all ledger writes - including Party creation -
 * to one at a time. The {@code phone_number} unique constraint remains as a backstop.
 */
public interface PartyRepository extends JpaRepository<PartyEntity, UUID> {
  Optional<PartyEntity> findByPhoneNumber(String phoneNumber);
}
