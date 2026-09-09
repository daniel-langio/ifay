package vendredi.soir.ifay.endpoint;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.ifay.endpoint.exception.BadRequestException;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.service.PaymentService;

/** Client-facing: a payer's claim that they paid a given psp reference, and polling its status. */
@RestController
@RequestMapping("/payments/claims")
@AllArgsConstructor
public class PaymentController {
  private final PaymentService paymentService;
  private final ApiKeyAuthorizer apiKeyAuthorizer;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentResponse create(
      @RequestHeader("X-Api-Key") String apiKey, @RequestBody CreateClaimRequest r) {
    apiKeyAuthorizer.acceptClient(apiKey);
    validate(r);
    return toResponse(
        paymentService.recordClaim(r.sender(), r.receiver(), r.amount(), r.type(), r.pspRef()));
  }

  @GetMapping("/{id}")
  public PaymentResponse get(@RequestHeader("X-Api-Key") String apiKey, @PathVariable String id) {
    apiKeyAuthorizer.acceptClient(apiKey);
    return toResponse(paymentService.getPayment(id));
  }

  private void validate(CreateClaimRequest r) {
    if (r == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (r.sender() == null || r.sender().isBlank()) {
      throw new BadRequestException("sender is required");
    }
    if (r.receiver() == null || r.receiver().isBlank()) {
      throw new BadRequestException("receiver is required");
    }
    if (r.amount() == null || r.amount() <= 0) {
      throw new BadRequestException("amount must be strictly positive");
    }
    if (r.type() == null) {
      throw new BadRequestException("type is required");
    }
    if (r.pspRef() == null || r.pspRef().isBlank()) {
      throw new BadRequestException("pspRef is required");
    }
  }

  private PaymentResponse toResponse(Payment p) {
    return new PaymentResponse(p.id(), p.isVerified() ? "VERIFIED" : "PENDING", p.effectiveAmount());
  }

  public record CreateClaimRequest(
      String sender, String receiver, Long amount, Provider type, String pspRef) {}

  public record PaymentResponse(String id, String status, Long amount) {}
}
