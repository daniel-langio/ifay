package vendredi.soir.ifay.endpoint;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.repository.ChainTipRepository;
import vendredi.soir.ifay.repository.PartyRepository;
import vendredi.soir.ifay.repository.PaymentEventEntity;
import vendredi.soir.ifay.repository.PaymentEventRepository;
import vendredi.soir.ifay.repository.VerifierRepository;

/**
 * The tamper-evidence surface: `/anchor` is deliberately public (no API key) - proving a record
 * wasn't altered only means something if anyone, not just ifay's own clients, can independently
 * fetch and check the current chain tip against a previously published anchor (see the daily
 * anchoring job that commits this to git). `/events`, which contains phone numbers and amounts,
 * stays behind the client API key - a third party is handed the proof by the client app, rather
 * than being able to enumerate every payment's raw event history directly from ifay.
 */
@RestController
@RequestMapping("/ledger")
@AllArgsConstructor
public class LedgerController {
  private final ChainTipRepository chainTipRepository;
  private final PaymentEventRepository paymentEventRepository;
  private final PartyRepository partyRepository;
  private final VerifierRepository verifierRepository;
  private final ApiKeyAuthorizer apiKeyAuthorizer;

  @GetMapping("/anchor")
  public AnchorResponse anchor() {
    var tip =
        chainTipRepository
            .findById(1L)
            .orElseThrow(() -> new IllegalStateException("Chain tip row missing"));
    return new AnchorResponse(tip.getSequence(), tip.getTipHash());
  }

  @GetMapping("/events")
  public List<EventResponse> events(
      @RequestHeader("X-Api-Key") String apiKey,
      @RequestParam Provider type,
      @RequestParam String pspRef) {
    apiKeyAuthorizer.acceptClient(apiKey);
    return paymentEventRepository
        .findByTypeAndPspRefOrderBySequenceAsc(type, pspRef.trim().toUpperCase())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private EventResponse toResponse(PaymentEventEntity e) {
    return new EventResponse(
        e.getSequence(),
        e.getEventType().name(),
        phoneOf(e.getSenderId()),
        phoneOf(e.getReceiverId()),
        e.getClaimedAmount(),
        verifierOf(e.getVerifierId()),
        e.getConfirmedAmount(),
        e.getOccurredAt() != null ? e.getOccurredAt().toString() : null,
        e.getPreviousEventHash(),
        e.getEventHash());
  }

  private String phoneOf(UUID partyId) {
    return partyId == null
        ? null
        : partyRepository.findById(partyId).map(p -> p.getPhoneNumber()).orElse(null);
  }

  private String verifierOf(UUID verifierId) {
    if (verifierId == null) {
      return null;
    }
    return verifierRepository
        .findById(verifierId)
        .map(v -> v.getAppId() + "@" + v.getVersion() + (v.getRevision() != null ? "+" + v.getRevision() : ""))
        .orElse(null);
  }

  public record AnchorResponse(long sequence, String tipHash) {}

  public record EventResponse(
      long sequence,
      String eventType,
      String senderPhone,
      String receiverPhone,
      Long claimedAmount,
      String verifier,
      Long confirmedAmount,
      String occurredAt,
      String previousEventHash,
      String eventHash) {}
}
