package vendredi.soir.ifay.endpoint;

import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.ifay.endpoint.exception.BadRequestException;
import vendredi.soir.ifay.service.SenderApiKeyService;

/**
 * Mints a static, per-sender API key, scoping subsequent {@code /payments/sent} list calls to one
 * sender. Mirrors {@link ReceiverController} exactly - same request/response shape, same trust
 * model (gated by the shared verifier key, not proof of phone ownership).
 */
@RestController
@RequestMapping("/senders")
@AllArgsConstructor
public class SenderController {
  private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

  private final SenderApiKeyService senderApiKeyService;
  private final ApiKeyAuthorizer apiKeyAuthorizer;

  @PostMapping("/api-keys")
  @ResponseStatus(HttpStatus.CREATED)
  public SenderApiKeyResponse createApiKey(
      @RequestHeader("X-Api-Key") String apiKey, @RequestBody CreateSenderApiKeyRequest r) {
    apiKeyAuthorizer.acceptVerifier(apiKey);
    if (r == null || r.phoneNumber() == null || !E164.matcher(r.phoneNumber()).matches()) {
      throw new BadRequestException("phoneNumber must be an E.164 phone number, e.g. +261341234567");
    }
    return new SenderApiKeyResponse(senderApiKeyService.mint(r.phoneNumber()));
  }

  public record CreateSenderApiKeyRequest(String phoneNumber) {}

  public record SenderApiKeyResponse(String senderApiKey) {}
}
