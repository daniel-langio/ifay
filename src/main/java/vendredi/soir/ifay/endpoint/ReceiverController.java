package vendredi.soir.ifay.endpoint;

import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.ifay.endpoint.exception.BadRequestException;
import vendredi.soir.ifay.service.ReceiverApiKeyService;

/**
 * Mints a static, per-receiver API key, scoping subsequent {@code /payments} list/verify calls to
 * one receiver. The phone number travels in the request body, never in the URL - a path segment
 * ends up in access logs and request-tracing infra by default, a body field doesn't.
 *
 * <p><b>Trust model:</b> gated by the same shared verifier key as {@code /payments/reports}
 * ({@link ApiKeyAuthorizer#acceptVerifier}), not by any proof that the caller actually owns the
 * phone number being registered - ifay has no OTP/SMS-sending capability to do better. Any
 * legitimate porofo install can mint a key for any phone number. This is a known, accepted
 * limitation, not an oversight: what the resulting key still buys is real scoping once minted, it
 * can only ever act on that one receiver's payments.
 */
@RestController
@RequestMapping("/receivers")
@AllArgsConstructor
public class ReceiverController {
  private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

  private final ReceiverApiKeyService receiverApiKeyService;
  private final ApiKeyAuthorizer apiKeyAuthorizer;

  @PostMapping("/api-keys")
  @ResponseStatus(HttpStatus.CREATED)
  public ReceiverApiKeyResponse createApiKey(
      @RequestHeader("X-Api-Key") String apiKey, @RequestBody CreateReceiverApiKeyRequest r) {
    apiKeyAuthorizer.acceptVerifier(apiKey);
    if (r == null || r.phoneNumber() == null || !E164.matcher(r.phoneNumber()).matches()) {
      throw new BadRequestException("phoneNumber must be an E.164 phone number, e.g. +261341234567");
    }
    return new ReceiverApiKeyResponse(receiverApiKeyService.mint(r.phoneNumber()));
  }

  public record CreateReceiverApiKeyRequest(String phoneNumber) {}

  public record ReceiverApiKeyResponse(String receiverApiKey) {}
}
