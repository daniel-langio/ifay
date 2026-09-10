package vendredi.soir.ifay.endpoint;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.ifay.endpoint.exception.BadRequestException;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.service.PaymentService;

/**
 * Verifier-facing: a verifier app (e.g. porofo, running its own on-device SMS parsing) reporting
 * a payment it directly observed. Deliberately takes already-parsed, already-verified fields
 * rather than raw evidence (a raw SMS string, say) - ifay has no way to confirm a raw payload
 * was genuinely produced by on-device verification rather than fabricated by whoever holds the
 * verifier API key, so it can only ever be as trustworthy as that verifier's own attestation.
 */
@RestController
@RequestMapping("/payments/reports")
@AllArgsConstructor
public class PaymentReportController {
  private final PaymentService paymentService;
  private final ApiKeyAuthorizer apiKeyAuthorizer;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentController.PaymentResponse create(
      @RequestHeader("X-Api-Key") String apiKey, @RequestBody CreateReportRequest r) {
    apiKeyAuthorizer.acceptVerifier(apiKey);
    validate(r);
    Payment payment =
        paymentService.recordReport(
            r.type(),
            r.pspRef(),
            r.amount(),
            r.verifier().appId(),
            r.verifier().version(),
            r.verifier().revision(),
            r.verificationType());
    return new PaymentController.PaymentResponse(
        payment.id(), payment.isVerified() ? "VERIFIED" : "PENDING", payment.effectiveAmount());
  }

  private void validate(CreateReportRequest r) {
    if (r == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (r.type() == null) {
      throw new BadRequestException("type is required");
    }
    if (r.pspRef() == null || r.pspRef().isBlank()) {
      throw new BadRequestException("pspRef is required");
    }
    if (r.amount() == null || r.amount() <= 0) {
      throw new BadRequestException("amount must be strictly positive");
    }
    if (r.verifier() == null
        || r.verifier().appId() == null
        || r.verifier().appId().isBlank()) {
      throw new BadRequestException("verifier.appId is required");
    }
    if (r.verifier().version() == null || r.verifier().version().isBlank()) {
      throw new BadRequestException("verifier.version is required");
    }
  }

  public record VerifierInfo(String appId, String version, String revision) {}

  /**
   * {@code verificationType} is an opaque string the verifier app supplies (e.g. "SMS_AUTO") -
   * ifay stores it as-is and defaults it to "SMS_AUTO" server-side if omitted, so older/other
   * verifier callers that don't send it yet stay compatible.
   */
  public record CreateReportRequest(
      Provider type, String pspRef, Long amount, VerifierInfo verifier, String verificationType) {}
}
