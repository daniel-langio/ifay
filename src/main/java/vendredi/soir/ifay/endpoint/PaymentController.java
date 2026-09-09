package vendredi.soir.ifay.endpoint;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.ifay.endpoint.exception.BadRequestException;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.VerificationStatus;
import vendredi.soir.ifay.service.PaymentService;

@RestController
@RequestMapping("/payments")
@AllArgsConstructor
public class PaymentController {
  private final PaymentService paymentService;
  private final ApiKeyAuthorizer apiKeyAuthorizer;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentResponse create(
      @RequestHeader("X-Api-Key") String apiKey, @RequestBody CreatePaymentRequest r) {
    apiKeyAuthorizer.accept(apiKey);
    validate(r);
    return toResponse(
        paymentService.createPayment(r.payerReference(), r.payerMsisdn(), r.amount(), r.scope()));
  }

  @GetMapping("/{id}")
  public PaymentResponse get(@RequestHeader("X-Api-Key") String apiKey, @PathVariable String id) {
    apiKeyAuthorizer.accept(apiKey);
    return toResponse(paymentService.getPayment(id));
  }

  private void validate(CreatePaymentRequest r) {
    if (r == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (r.payerReference() == null || r.payerReference().isBlank()) {
      throw new BadRequestException("payerReference is required");
    }
    if (r.payerMsisdn() == null || r.payerMsisdn().isBlank()) {
      throw new BadRequestException("payerMsisdn is required");
    }
    if (r.amount() == null || r.amount() <= 0) {
      throw new BadRequestException("amount must be strictly positive");
    }
  }

  private PaymentResponse toResponse(Payment p) {
    VerificationStatus status = p.getVerificationStatus();
    return new PaymentResponse(
        p.id(),
        status,
        p.amountRequested(),
        status == VerificationStatus.SUCCEEDED ? p.confirmedAmount() : null);
  }

  public record CreatePaymentRequest(
      String payerReference, String payerMsisdn, Long amount, String scope) {}

  public record PaymentResponse(
      String id, VerificationStatus status, long amountRequested, Long confirmedAmount) {}
}
