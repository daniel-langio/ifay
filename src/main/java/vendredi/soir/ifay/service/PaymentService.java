package vendredi.soir.ifay.service;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.ifay.endpoint.exception.NotFoundException;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.mvola.MvolaApiClient;
import vendredi.soir.ifay.mvola.MvolaTransactionStatusResponse;
import vendredi.soir.ifay.repository.PaymentEntity;
import vendredi.soir.ifay.repository.PaymentMapper;
import vendredi.soir.ifay.repository.PaymentRepository;

/**
 * No background retry/event queue - unlike a Lambda-based system, this is a plain deployable
 * Spring Boot app with no scheduler, so verification is lazy: every read of a still-verifying
 * payment (see {@link #getPayment}) re-checks MVola's status endpoint fresh, exactly once per
 * call, capped at Payment.MAX_VERIFICATION_ATTEMPT_NB total attempts across all reads.
 */
@Service
@AllArgsConstructor
public class PaymentService {
  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;
  private final MvolaApiClient mvolaApiClient;

  @Transactional
  public Payment createPayment(String payerReference, String payerMsisdn, long amount, String scope) {
    var id = UUID.randomUUID();
    var initiated = mvolaApiClient.initiatePayment(payerMsisdn, amount, id.toString());

    var payment =
        Payment.builder()
            .id(id.toString())
            .payerReference(payerReference)
            .payerMsisdn(payerMsisdn)
            .amountRequested(amount)
            .serverCorrelationId(initiated.serverCorrelationId())
            .scope(scope)
            .creationInstant(Instant.now())
            .verificationAttemptNb(0)
            .build();

    paymentRepository.save(paymentMapper.toEntity(payment));
    return payment;
  }

  @Transactional
  public Payment getPayment(String id) {
    PaymentEntity entity =
        paymentRepository
            .findById(UUID.fromString(id))
            .orElseThrow(() -> new NotFoundException("No payment with id " + id));
    Payment payment = paymentMapper.toDomain(entity);

    if (payment.getVerificationStatus() != vendredi.soir.ifay.model.VerificationStatus.VERIFYING) {
      return payment;
    }

    MvolaTransactionStatusResponse status = mvolaApiClient.statusOf(payment.serverCorrelationId());
    // MVola's status response may or may not echo the amount back - if it confirms completion
    // without one, trust what we originally asked for rather than leave the payment stuck
    // VERIFYING forever with a null confirmedAmount.
    Long confirmedAmount = null;
    if (status.isCompleted()) {
      confirmedAmount = status.amount() != null ? Long.valueOf(status.amount()) : payment.amountRequested();
    }
    var updated =
        payment.toBuilder()
            .lastVerificationInstant(Instant.now())
            .verificationAttemptNb(payment.verificationAttemptNb() + 1)
            .pspReportedFailure(status.isFailed())
            .confirmedAmount(confirmedAmount)
            .build();

    paymentRepository.save(paymentMapper.toEntity(updated));
    return updated;
  }
}
