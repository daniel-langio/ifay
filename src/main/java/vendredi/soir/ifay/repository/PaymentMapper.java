package vendredi.soir.ifay.repository;

import org.springframework.stereotype.Component;
import vendredi.soir.ifay.model.Payment;

@Component
public class PaymentMapper {

  public Payment toDomain(PaymentEntity e) {
    return Payment.builder()
        .id(e.getId().toString())
        .payerReference(e.getPayerReference())
        .payerMsisdn(e.getPayerMsisdn())
        .amountRequested(e.getAmountRequested())
        .confirmedAmount(e.getConfirmedAmount())
        .pspReportedFailure(e.isPspReportedFailure())
        .serverCorrelationId(e.getServerCorrelationId())
        .scope(e.getScope())
        .creationInstant(e.getCreationInstant())
        .lastVerificationInstant(e.getLastVerificationInstant())
        .verificationAttemptNb(e.getVerificationAttemptNb())
        .build();
  }

  public PaymentEntity toEntity(Payment p) {
    return PaymentEntity.builder()
        .id(java.util.UUID.fromString(p.id()))
        .payerReference(p.payerReference())
        .payerMsisdn(p.payerMsisdn())
        .amountRequested(p.amountRequested())
        .confirmedAmount(p.confirmedAmount())
        .pspReportedFailure(p.pspReportedFailure())
        .serverCorrelationId(p.serverCorrelationId())
        .scope(p.scope())
        .creationInstant(p.creationInstant())
        .lastVerificationInstant(p.lastVerificationInstant())
        .verificationAttemptNb(p.verificationAttemptNb())
        .build();
  }
}
