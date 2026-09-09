package vendredi.soir.ifay.service;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import vendredi.soir.ifay.repository.PartyEntity;
import vendredi.soir.ifay.repository.PartyRepository;

/**
 * Not race-safe on its own - callers must already hold the {@code ChainTip} lock (see
 * {@link PaymentTransactions}), which serializes this along with everything else a ledger write
 * touches. The {@code phone_number} unique constraint is a backstop, not the primary guard.
 */
@Component
@AllArgsConstructor
class PartyService {
  private final PartyRepository partyRepository;

  UUID findOrCreate(String phoneNumber) {
    return partyRepository
        .findByPhoneNumber(phoneNumber)
        .orElseGet(
            () ->
                partyRepository.save(
                    PartyEntity.builder().id(UUID.randomUUID()).phoneNumber(phoneNumber).build()))
        .getId();
  }
}
