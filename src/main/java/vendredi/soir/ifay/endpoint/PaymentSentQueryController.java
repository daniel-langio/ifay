package vendredi.soir.ifay.endpoint;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.service.PaymentService;

/**
 * Sender-facing: list one sender's own payments - the other side of {@link
 * PaymentQueryController}. Scoped by {@link SenderApiKeyAuthorizer} alone, never by a phone
 * number the caller passes in.
 *
 * <p>Deliberately no manual-verify counterpart here: a claim already sets {@code sentAt}
 * unconditionally the moment it's recorded (see {@code PaymentTransactions#recordClaim}), so
 * there's nothing left for a sender to self-attest - the only field still missing for
 * verification is {@code receivedAt}, which only the receiver (or an independent verifier report,
 * e.g. porofo reading its own "sent" SMS) is positioned to supply. A sender self-attesting that
 * the other side received the money would defeat the point of independent verification.
 */
@RestController
@RequestMapping("/payments/sent")
@AllArgsConstructor
public class PaymentSentQueryController {
  private final PaymentService paymentService;
  private final SenderApiKeyAuthorizer senderApiKeyAuthorizer;

  @GetMapping
  public List<PaymentSentListItemResponse> list(
      @RequestHeader("X-Api-Key") String apiKey, @RequestParam(required = false) String verified) {
    UUID senderId = senderApiKeyAuthorizer.acceptSender(apiKey);
    return paymentService.listForSender(senderId, parseVerifiedFilter(verified)).stream()
        .map(PaymentSentQueryController::toListItem)
        .toList();
  }

  /** {@code null} or "all" selects every payment; "true"/"false" filters to one state. */
  private static Boolean parseVerifiedFilter(String verified) {
    if (verified == null || verified.equalsIgnoreCase("all")) {
      return null;
    }
    return Boolean.parseBoolean(verified);
  }

  private static PaymentSentListItemResponse toListItem(Payment p) {
    return new PaymentSentListItemResponse(
        p.id(),
        p.type(),
        p.pspRef(),
        p.effectiveAmount(),
        p.isVerified(),
        p.verificationType(),
        p.receiver() != null ? p.receiver().phoneNumber() : null);
  }

  public record PaymentSentListItemResponse(
      String id,
      Provider type,
      String pspRef,
      Long amount,
      boolean verified,
      String verificationType,
      String receiverPhone) {}
}
