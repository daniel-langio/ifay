package vendredi.soir.ifay.repository;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import vendredi.soir.ifay.model.Party;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.Verifier;

@Component
@AllArgsConstructor
public class PaymentMapper {
  private final PartyRepository partyRepository;
  private final VerifierRepository verifierRepository;

  public Payment toDomain(PaymentEntity e) {
    return Payment.builder()
        .id(e.getId().toString())
        .pspRef(e.getPspRef())
        .type(e.getType())
        .sender(e.getSenderId() != null ? toParty(e.getSenderId()) : null)
        .receiver(e.getReceiverId() != null ? toParty(e.getReceiverId()) : null)
        .claimedAmount(e.getClaimedAmount())
        .confirmedAmount(e.getConfirmedAmount())
        .verifier(e.getVerifierId() != null ? toVerifier(e.getVerifierId()) : null)
        .sentAt(e.getSentAt())
        .receivedAt(e.getReceivedAt())
        .verifiedAt(e.getVerifiedAt())
        .verificationType(e.getVerificationType())
        .build();
  }

  private Party toParty(java.util.UUID id) {
    PartyEntity p =
        partyRepository.findById(id).orElseThrow(() -> new IllegalStateException("No party " + id));
    return new Party(p.getId().toString(), p.getPhoneNumber());
  }

  private Verifier toVerifier(java.util.UUID id) {
    VerifierEntity v =
        verifierRepository
            .findById(id)
            .orElseThrow(() -> new IllegalStateException("No verifier " + id));
    return new Verifier(v.getId().toString(), v.getAppId(), v.getVersion(), v.getRevision());
  }
}
