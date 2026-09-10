package vendredi.soir.ifay.endpoint;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.service.PaymentService;

/**
 * Receiver-facing: list one receiver's own payments, and let them vouch for one manually without
 * an independent verifier report. Both endpoints are scoped by {@link ReceiverApiKeyAuthorizer},
 * never by a phone number or any other value the caller passes in - the API key alone determines
 * which receiver's payments are visible/actionable.
 */
@RestController
@RequestMapping("/payments")
@AllArgsConstructor
public class PaymentQueryController {
  private final PaymentService paymentService;
  private final ReceiverApiKeyAuthorizer receiverApiKeyAuthorizer;

  @GetMapping
  public List<PaymentListItemResponse> list(
      @RequestHeader("X-Api-Key") String apiKey, @RequestParam(required = false) String verified) {
    UUID receiverId = receiverApiKeyAuthorizer.acceptReceiver(apiKey);
    return paymentService.listForReceiver(receiverId, parseVerifiedFilter(verified)).stream()
        .map(PaymentQueryController::toListItem)
        .toList();
  }

  @PostMapping("/{id}/verify")
  public PaymentListItemResponse verify(
      @RequestHeader("X-Api-Key") String apiKey, @PathVariable String id) {
    UUID receiverId = receiverApiKeyAuthorizer.acceptReceiver(apiKey);
    return toListItem(paymentService.recordManualVerification(id, receiverId));
  }

  /** {@code null} or "all" selects every payment; "true"/"false" filters to one state. */
  private static Boolean parseVerifiedFilter(String verified) {
    if (verified == null || verified.equalsIgnoreCase("all")) {
      return null;
    }
    return Boolean.parseBoolean(verified);
  }

  private static PaymentListItemResponse toListItem(Payment p) {
    return new PaymentListItemResponse(
        p.id(),
        p.type(),
        p.pspRef(),
        p.effectiveAmount(),
        p.isVerified(),
        p.verificationType(),
        p.sender() != null ? p.sender().phoneNumber() : null);
  }

  public record PaymentListItemResponse(
      String id,
      Provider type,
      String pspRef,
      Long amount,
      boolean verified,
      String verificationType,
      String senderPhone) {}
}
