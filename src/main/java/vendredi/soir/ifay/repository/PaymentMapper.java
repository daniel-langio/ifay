package vendredi.soir.ifay.repository;

import org.springframework.stereotype.Component;
import vendredi.soir.ifay.model.Payment;

@Component
public class PaymentMapper {

  public Payment toDomain(PaymentEntity e) {
    return Payment.builder()
        .id(e.getId().toString())
        .pspRef(e.getPspRef())
        .type(e.getType())
        .sender(e.getSender())
        .receiver(e.getReceiver())
        .claimedAmount(e.getClaimedAmount())
        .confirmedAmount(e.getConfirmedAmount())
        .verifier(e.getVerifier())
        .verifierRevision(e.getVerifierRevision())
        .sentAt(e.getSentAt())
        .receivedAt(e.getReceivedAt())
        .verifiedAt(e.getVerifiedAt())
        .build();
  }

  public PaymentEntity toEntity(Payment p) {
    return PaymentEntity.builder()
        .id(java.util.UUID.fromString(p.id()))
        .pspRef(p.pspRef())
        .type(p.type())
        .sender(p.sender())
        .receiver(p.receiver())
        .claimedAmount(p.claimedAmount())
        .confirmedAmount(p.confirmedAmount())
        .verifier(p.verifier())
        .verifierRevision(p.verifierRevision())
        .sentAt(p.sentAt())
        .receivedAt(p.receivedAt())
        .verifiedAt(p.verifiedAt())
        .build();
  }
}
