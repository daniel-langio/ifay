package vendredi.soir.ifay.endpoint;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.ifay.endpoint.exception.BadRequestException;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.service.PaymentService;

/**
 * Sender-facing: list one sender's own payments - the other side of {@link
 * PaymentQueryController}. Scoped by {@link SenderApiKeyAuthorizer} alone, never by a phone
 * number the caller passes in.
 *
 * <p>A claim already sets {@code sentAt} unconditionally the moment it's recorded (see {@code
 * PaymentTransactions#recordClaim}), and normally already carries its {@code pspRef} too - the
 * only field still missing for verification is {@code receivedAt}, which only the receiver (or an
 * independent verifier report, e.g. porofo reading its own "sent" SMS) is positioned to supply. A
 * claim may, however, be recorded as a "to-send" claim with no {@code pspRef} yet (the payer
 * hasn't paid at all when the claim is made) - {@link #verify} is how the sender later attaches
 * the real ref once they've actually paid. That action is self-attested (same trust level as
 * {@code PaymentQueryController#verify}'s receiver-side manual verify) - see the TODO on {@code
 * PaymentTransactions#recordSenderRefVerification} for why that's a stopgap.
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

  /** Attaches {@code pspRef} to a "to-send" claim (one recorded with no ref yet) and verifies
   *  it - see the class doc for the trust caveat. Not meant for a claim that already has a ref;
   *  use {@code PaymentQueryController#verify} on the receiver side for that case instead. */
  @PostMapping("/{id}/verify")
  public PaymentSentListItemResponse verify(
      @RequestHeader("X-Api-Key") String apiKey,
      @PathVariable String id,
      @RequestBody VerifySentPaymentRequest r) {
    UUID senderId = senderApiKeyAuthorizer.acceptSender(apiKey);
    if (r == null || r.pspRef() == null || r.pspRef().isBlank()) {
      throw new BadRequestException("pspRef is required");
    }
    return toListItem(paymentService.recordSenderRefVerification(id, senderId, r.pspRef()));
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

  public record VerifySentPaymentRequest(String pspRef) {}
}
