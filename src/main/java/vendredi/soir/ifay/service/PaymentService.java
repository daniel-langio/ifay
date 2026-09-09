package vendredi.soir.ifay.service;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.ifay.endpoint.exception.NotFoundException;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.repository.PaymentMapper;
import vendredi.soir.ifay.repository.PaymentRepository;

/**
 * Records claims (from a client app, on behalf of a payer) and reports (from a verifier, e.g.
 * porofo's SMS parser) against the same {@code (type, pspRef)} key - whichever side arrives
 * first creates the row, the other side updates it and triggers verification. All the actual
 * work - including the race-safety - lives in {@link PaymentTransactions}, which locks the
 * global chain-tip row first and does everything else (Party/Verifier lookup-or-create, the
 * Payment row upsert, the hash-chained event append) inside that one critical section.
 */
@Service
@AllArgsConstructor
public class PaymentService {
  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;
  private final PaymentTransactions transactions;

  public Payment recordClaim(
      String senderPhone, String receiverPhone, long amount, Provider type, String rawPspRef) {
    return transactions.recordClaim(
        type, normalizeRef(rawPspRef), senderPhone, receiverPhone, amount);
  }

  public Payment recordReport(
      Provider type,
      String rawPspRef,
      long amount,
      String verifierAppId,
      String verifierVersion,
      String verifierRevision) {
    return transactions.recordReport(
        type, normalizeRef(rawPspRef), amount, verifierAppId, verifierVersion, verifierRevision);
  }

  /**
   * Case matters for matching (a player types a Ref/Trans Id in whatever case their own
   * confirmation shows it, while a verifier's own SMS parser may normalize text to lowercase
   * before extracting one) - normalize both sides identically so a case difference alone never
   * causes an otherwise-correct match to miss.
   */
  private static String normalizeRef(String rawPspRef) {
    return rawPspRef.trim().toUpperCase();
  }

  @Transactional(readOnly = true)
  public Payment getPayment(String id) {
    return paymentMapper.toDomain(
        paymentRepository
            .findById(UUID.fromString(id))
            .orElseThrow(() -> new NotFoundException("No payment with id " + id)));
  }
}
